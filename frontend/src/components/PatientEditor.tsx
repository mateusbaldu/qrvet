import { useEffect, useState } from 'react'
import { PawPrint } from 'lucide-react'
import { Editor, type Field } from './ClinicUI'
import { ApiError, readableError, request } from '../services/api'
import { tutorFields, uploadPhoto } from '../services/patientForm'
import { clinicApi, localDate, searchText, type Paciente, type Tutor } from '../services/clinic'

export function PatientAvatar({ patient, large = false }: { patient: Paciente; large?: boolean }) {
  const [photo, setPhoto] = useState<{ key: string; url: string } | null>(null)
  const key = patient.id + ':' + patient.fotoVersao
  useEffect(() => {
    let active = true,
      url = ''
    if (patient.fotoVersao)
      request<Blob>('/pacientes/' + patient.id + '/foto', { responseType: 'blob' })
        .then(blob => {
          if (active) {
            url = URL.createObjectURL(blob)
            setPhoto({ key, url })
          }
        })
        .catch(() => {
          /* A identificação textual continua disponível se a foto não carregar. */
        })
    return () => {
      active = false
      if (url) URL.revokeObjectURL(url)
    }
  }, [patient.id, patient.fotoVersao, key])
  return (
    <span className={'clinic-avatar' + (large ? ' large' : '')}>
      {photo?.key === key ? (
        <img src={photo.url} alt={'Foto de ' + patient.nome} />
      ) : (
        <PawPrint size={large ? 30 : 20} />
      )}
    </span>
  )
}

export function PhotoInput({ onChange }: { onChange: (file: File | null) => void }) {
  const [preview, setPreview] = useState('')
  const [error, setError] = useState('')
  useEffect(
    () => () => {
      if (preview) URL.revokeObjectURL(preview)
    },
    [preview]
  )
  return (
    <div className="clinic-photo-input">
      <label>
        Foto do paciente (opcional)
        <input
          name="foto"
          type="file"
          accept="image/jpeg,image/png"
          onChange={e => {
            const file = e.target.files?.[0] ?? null
            setError('')
            setPreview('')
            onChange(null)
            if (
              file &&
              (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 5 * 1024 * 1024)
            ) {
              setError('Escolha uma foto JPG ou PNG de até 5 MB.')
              e.target.value = ''
              return
            }
            if (file) {
              setPreview(URL.createObjectURL(file))
              onChange(file)
            }
          }}
        />
        <small>JPG ou PNG, até 5 MB e 16 megapixels. A foto fica salva no servidor local.</small>
      </label>
      {preview && <img src={preview} alt="Prévia da foto selecionada" />}
      {error && (
        <p className="clinic-error" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}

export function PatientEditor({
  tutors,
  patient,
  tutorId = '',
  onClose,
  onSaved,
}: {
  tutors: Tutor[]
  patient?: Paciente
  tutorId?: string
  onClose: () => void
  onSaved: (patient: Paciente, openAdmission: boolean, warning: string) => void
}) {
  const [newTutor, setNewTutor] = useState(!tutors.length)
  const [search, setSearch] = useState('')
  const [selectedTutor, setSelectedTutor] = useState(String(patient?.tutorId ?? tutorId))
  const [photo, setPhoto] = useState<File | null>(null)
  const [admit, setAdmit] = useState(false)
  const fields: Field[] = [
    { name: 'nome', label: 'Nome do paciente', maxLength: 100 },
    { name: 'especie', label: 'Espécie', maxLength: 50, hint: 'Ex.: Canina, Felina, Ave' },
    {
      name: 'raca',
      label: 'Raça',
      maxLength: 50,
      hint: 'Use SRD quando não houver raça definida.',
    },
    {
      name: 'sexo',
      label: 'Sexo',
      options: ['Macho', 'Fêmea', 'Não identificado'].map(value => ({ value, label: value })),
    },
    {
      name: 'dataNascimento',
      label: 'Data de nascimento (pode ser estimada)',
      type: 'date',
      max: localDate(),
    },
    { name: 'peso', label: 'Peso (kg)', type: 'number', min: '0.01', max: '9999.99', step: '0.01' },
    { name: 'observacoes', label: 'Observações', type: 'textarea', required: false },
  ].map(field => ({
    ...field,
    value: patient ? String(patient[field.name as keyof Paciente] ?? '') : undefined,
  }))
  if (newTutor) fields.push(...tutorFields.map(f => ({ ...f, name: 'tutor_' + f.name })))
  return (
    <Editor
      title={patient ? 'Editar paciente' : 'Cadastrar paciente'}
      description="Cadastre o paciente e seu responsável juntos. Os campos com * são obrigatórios."
      fields={fields}
      onCancel={onClose}
      closeOnSave={false}
      beforeFields={
        <section className="clinic-form-section">
          <h3>Tutor responsável</h3>
          <div className="clinic-mode-switch" role="group" aria-label="Responsável pelo paciente">
            <button
              type="button"
              aria-pressed={!newTutor}
              disabled={!tutors.length}
              onClick={() => setNewTutor(false)}
            >
              Tutor já cadastrado
            </button>
            <button type="button" aria-pressed={newTutor} onClick={() => setNewTutor(true)}>
              Cadastrar novo tutor
            </button>
          </div>
          {newTutor ? (
            <p>Preencha os dados do tutor abaixo dos dados do paciente. Tudo será salvo junto.</p>
          ) : (
            <>
              <label>
                Buscar tutor por nome ou CPF
                <input
                  type="search"
                  value={search}
                  onChange={e => setSearch(e.target.value)}
                  placeholder="Nome ou CPF do responsável"
                />
              </label>
              <label>
                Tutor responsável *
                <select
                  name="tutorId"
                  required
                  value={selectedTutor}
                  onChange={e => setSelectedTutor(e.target.value)}
                >
                  <option value="">Selecione</option>
                  {tutors
                    .filter(
                      t =>
                        String(t.id) === selectedTutor ||
                        searchText(t.nome + ' ' + t.cpf).includes(searchText(search))
                    )
                    .map(t => (
                      <option key={t.id} value={t.id}>
                        {t.nome} · {t.cpf}
                      </option>
                    ))}
                </select>
              </label>
            </>
          )}
        </section>
      }
      afterFields={
        <>
          <PhotoInput onChange={setPhoto} />
          {!patient && (
            <label className="clinic-checkbox">
              <input type="checkbox" checked={admit} onChange={e => setAdmit(e.target.checked)} />
              Abrir internação após salvar
            </label>
          )}
        </>
      }
      onSave={async values => {
        const data: Record<string, unknown> = Object.fromEntries(
          Object.entries(values).filter(([name]) => !name.startsWith('tutor_'))
        )
        if (newTutor)
          data.novoTutor = Object.fromEntries(
            tutorFields.map(f => [f.name, values['tutor_' + f.name]])
          )
        else data.tutorId = Number(selectedTutor)
        let saved: Paciente
        try {
          saved = await clinicApi.save<Paciente>(
            patient ? '/pacientes/' + patient.id : '/pacientes',
            data,
            patient ? 'PUT' : 'POST'
          )
        } catch (error) {
          if (error instanceof ApiError)
            error.fields = Object.fromEntries(
              Object.entries(error.fields).map(([key, value]) => [
                key.replace('novoTutor.', 'tutor_'),
                value,
              ])
            )
          throw error
        }
        let warning = ''
        if (photo)
          try {
            saved = await uploadPhoto(saved.id, photo)
          } catch (error) {
            warning = 'Cadastro salvo, mas a foto não foi enviada. ' + readableError(error)
          }
        onSaved(saved, admit && !warning, warning)
      }}
    />
  )
}
