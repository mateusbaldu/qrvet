import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ArrowRight, Plus, Search, UserRound } from 'lucide-react'
import { useAuth } from '../auth/useAuth'
import { useResource } from '../hooks/useResource'
import { Editor, EmptyState, Notice, Page, Pagination, ResourceState, StatusBadge, type Field } from '../components/ClinicUI'
import { ApiError, request } from '../services/api'
import { PatientAvatar, PatientEditor, PhotoInput } from '../components/PatientEditor'
import { tutorFields, uploadPhoto } from '../services/patientForm'
import { allPages, clinicApi, dateOnly, dateTime, paginate, searchText, type Baia, type Internacao, type Paciente, type Tutor } from '../services/clinic'

export function Registry({ kind }: { kind: 'tutores' | 'pacientes' | 'baias' }) {
  const { user } = useAuth()
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [filter, setFilter] = useState('')
  const [search, setSearch] = useState('')
  const [success, setSuccess] = useState('')
  const [editing, setEditing] = useState<Baia | Tutor | 'new' | null>(params.has('novo') ? 'new' : null)
  const [statusEdit, setStatusEdit] = useState<Baia | null>(null)
  const tutorId = params.get('tutor')
  const resource = useResource(async () => {
    const items = await allPages<Tutor | Paciente | Baia>('/' + kind)
    const [tutors, admissions] = kind === 'pacientes' ? await Promise.all([allPages<Tutor>('/tutores'), allPages<Internacao>('/internacoes/ativas')]) : [[], []]
    return { items, tutors, admissions }
  }, kind)
  const title = { tutores: 'Tutores', pacientes: 'Pacientes', baias: 'Baias' }[kind]
  const editable = kind !== 'baias' || user?.role === 'ADMIN'
  const fields: Field[] = kind === 'tutores' ? tutorFields.map(field => ({ ...field, value: editing && editing !== 'new' ? String((editing as Tutor)[field.name as keyof Tutor] ?? '') : '' })) : [
    { name: 'identificacao', label: 'Identificação', maxLength: 50, value: editing && editing !== 'new' ? (editing as Baia).identificacao : '' },
    { name: 'observacao', label: 'Observação', required: false, maxLength: 255, value: editing && editing !== 'new' ? (editing as Baia).observacao ?? '' : '' },
    ...(editing === 'new' ? [{ name: 'status', label: 'Situação inicial', value: 'DISPONIVEL', options: [{ value: 'DISPONIVEL', label: 'Disponível' }, { value: 'MANUTENCAO', label: 'Manutenção' }] }] : []),
  ]
  const filtered = resource.data?.items.filter(item => {
    const term = searchText(search.trim())
    if ('identificacao' in item) return (!filter || item.status === filter) && searchText(item.identificacao + ' ' + (item.observacao ?? '')).includes(term)
    if ('especie' in item) {
      const admitted = resource.data!.admissions.some(i => i.pacienteId === item.id)
      const tutor = resource.data!.tutors.find(t => t.id === item.tutorId)
      return (!tutorId || item.tutorId === Number(tutorId)) && (!filter || (filter === 'ATIVA' ? admitted : !admitted)) && searchText([item.nome, item.especie, item.raca, tutor?.nome].join(' ')).includes(term)
    }
    return searchText([item.nome, item.cpf, item.email, item.telefone].join(' ')).includes(term)
  }) ?? []
  const result = paginate(filtered, page)
  const canCreate = editable && !!resource.data
  return <Page title={title} description={{ tutores: 'Os responsáveis que confiam seu cuidado à nossa clínica.', pacientes: 'Cada paciente, uma história. Encontre todas elas aqui.', baias: 'Organize os espaços e acompanhe a ocupação da clínica.' }[kind]} action={editable && <button className="clinic-primary" disabled={!canCreate} onClick={() => { setSuccess(''); setEditing('new') }}><Plus size={17} />{kind === 'baias' ? 'Nova baia' : kind === 'tutores' ? 'Novo tutor' : 'Cadastrar paciente'}</button>}>
    {success && <Notice>{success}</Notice>}
    {editing && kind === 'pacientes' && resource.data && <PatientEditor tutors={resource.data.tutors} tutorId={tutorId ?? ''} onClose={() => { setEditing(null); navigate(tutorId ? '/pacientes?tutor=' + tutorId : '/pacientes', { replace: true }) }} onSaved={(patient, admit, warning) => navigate(admit ? '/internacoes?paciente=' + patient.id : '/pacientes/' + patient.id, { state: { warning } })} />}
    {editing && kind !== 'pacientes' && <Editor key={typeof editing === 'string' ? editing : editing.id} title={editing === 'new' ? kind === 'tutores' ? 'Cadastrar tutor' : 'Nova baia' : kind === 'tutores' ? 'Editar tutor' : 'Editar baia'} description="Preencha os dados abaixo. Os campos com * são obrigatórios." fields={fields} onCancel={() => setEditing(null)} onSave={async data => {
      await clinicApi.save<Paciente | Tutor | Baia>(editing === 'new' ? '/' + kind : '/' + kind + '/' + editing.id, data, editing === 'new' ? 'POST' : 'PUT')
      setSuccess('Cadastro salvo com sucesso.'); resource.reload()
    }} />}
    {statusEdit && <Editor title={'Alterar situação · ' + statusEdit.identificacao} fields={[{ name: 'status', label: 'Situação', value: statusEdit.status, options: [{ value: 'DISPONIVEL', label: 'Disponível' }, { value: 'MANUTENCAO', label: 'Manutenção' }] }]} onCancel={() => setStatusEdit(null)} onSave={async data => { await clinicApi.save('/baias/' + statusEdit.id + '/status', data, 'PATCH'); setSuccess('Situação da baia atualizada.'); resource.reload() }} />}
    <div className="clinic-toolbar clinic-panel"><label className="clinic-search"><span>Buscar</span><span className="clinic-search-input"><Search size={18} /><input type="search" value={search} onChange={e => { setSearch(e.target.value); setPage(0) }} placeholder={kind === 'pacientes' ? 'Buscar por paciente ou tutor' : kind === 'tutores' ? 'Nome, CPF, telefone ou e-mail' : 'Identificação da baia'} /></span></label>
      {kind !== 'tutores' && <div className="clinic-filters" role="group" aria-label="Filtrar por situação">{(kind === 'pacientes' ? [['', 'Todos'], ['ATIVA', 'Internados'], ['SEM', 'Sem internação ativa']] : [['', 'Todas'], ['DISPONIVEL', 'Disponíveis'], ['OCUPADA', 'Ocupadas'], ['MANUTENCAO', 'Manutenção']]).map(([value, label]) => <button key={value} className={filter === value ? 'selected' : ''} aria-pressed={filter === value} onClick={() => { setFilter(value); setPage(0) }}>{label}</button>)}</div>}
      {tutorId && <Link to="/pacientes">Ver todos os pacientes</Link>}
    </div>
    <ResourceState {...resource} />
    {resource.data && <>
      {kind === 'pacientes' ? <section className="clinic-table-panel"><div className="clinic-table-scroll"><table className="clinic-table"><thead><tr><th>Paciente</th><th>Tutor</th><th>Situação</th><th>Peso</th><th><span className="visually-hidden">Ações</span></th></tr></thead><tbody>{result.items.map(item => {
        const p = item as Paciente
        const admission = resource.data!.admissions.find(i => i.pacienteId === p.id)
        return <tr key={p.id}><td><Link className="clinic-identity" to={'/pacientes/' + p.id}><PatientAvatar patient={p} /><span><strong>{p.nome}</strong><small>{p.especie} · {p.raca}</small></span></Link></td><td>{resource.data!.tutors.find(t => t.id === p.tutorId)?.nome ?? '—'}</td><td><StatusBadge status={admission ? 'ATIVA' : 'NEUTRAL'}>{admission ? 'Internado' : 'Sem internação ativa'}</StatusBadge></td><td>{p.peso.toLocaleString('pt-BR')} kg</td><td><Link className="clinic-row-link" aria-label={'Ver detalhes de ' + p.nome} to={'/pacientes/' + p.id}><ArrowRight size={18} /></Link></td></tr>
      })}</tbody></table></div>{!result.total && <EmptyState><p>Tente outro nome ou filtro para encontrar o paciente.</p></EmptyState>}</section> : <div className="clinic-grid">{result.items.map(item => <article className="clinic-card" key={item.id}>{'identificacao' in item ? <><StatusBadge status={item.status} /><h2>{item.identificacao}</h2><p>{item.observacao || 'Sem observações.'}</p>{editable && <div className="clinic-actions"><button onClick={() => setEditing(item)}>Editar</button><button disabled={item.status === 'OCUPADA'} title={item.status === 'OCUPADA' ? 'A baia será liberada ao encerrar a internação.' : undefined} onClick={() => setStatusEdit(item)}>Alterar situação</button></div>}</> : 'cpf' in item ? <><span className="clinic-avatar"><UserRound size={22} /></span><h2>{item.nome}</h2><dl className="clinic-details"><div><dt>CPF</dt><dd>{item.cpf}</dd></div><div><dt>Telefone</dt><dd>{item.telefone}</dd></div></dl><p>{item.email}<br />{item.endereco}</p><div className="clinic-actions"><button onClick={() => setEditing(item)}>Editar tutor</button><Link to={'/pacientes?novo=1&tutor=' + item.id}>Adicionar paciente</Link></div><Link to={'/pacientes?tutor=' + item.id}>Ver pacientes →</Link></> : null}</article>)}</div>}
      {kind !== 'pacientes' && !result.total && <EmptyState><p>Os novos cadastros aparecerão aqui.</p></EmptyState>}
      <Pagination {...result} onChange={setPage} />
    </>}
  </Page>
}

export function PatientDetail() {
  const { id = '' } = useParams()
  const location = useLocation()
  const [editing, setEditing] = useState(false)
  const [editingTutor, setEditingTutor] = useState(false)
  const [photoAction, setPhotoAction] = useState<'upload' | 'remove' | null>(null)
  const [photo, setPhoto] = useState<File | null>(null)
  const [success, setSuccess] = useState('')
  const [warning, setWarning] = useState<string>(location.state?.warning ?? '')
  const resource = useResource(async () => {
    const [patient, tutors, history] = await Promise.all([request<Paciente>('/pacientes/' + encodeURIComponent(id)), allPages<Tutor>('/tutores'), allPages<Internacao>('/internacoes?pacienteId=' + encodeURIComponent(id))])
    return { patient, tutors, tutor: tutors.find(t => t.id === patient.tutorId), history }
  }, id)
  const data = resource.data
  const active = data?.history.find(i => i.status === 'ATIVA')
  return <Page title={data?.patient.nome ?? 'Detalhes do paciente'} description="Identificação, responsável e histórico de internações." action={<Link className="clinic-button" to="/pacientes">Voltar aos pacientes</Link>}>
    {success && <Notice>{success}</Notice>}{warning && <p className="clinic-notice" role="status">{warning}</p>}
    <ResourceState {...resource} />
    {data && <><section className="clinic-patient-banner"><PatientAvatar patient={data.patient} large /><div><h2>{data.patient.nome}</h2><p>{data.patient.especie} · {data.patient.raca} · {data.patient.sexo}</p></div><StatusBadge status={active ? 'ATIVA' : 'NEUTRAL'}>{active ? 'Internado' : 'Sem internação ativa'}</StatusBadge><Link className="clinic-button clinic-primary" to={active ? '/internacoes/' + active.id : '/internacoes?paciente=' + id}>{active ? 'Acompanhar internação' : 'Abrir internação'}<ArrowRight size={16} /></Link></section>
      <div className="clinic-detail-grid"><div><section className="clinic-panel"><div className="clinic-section-heading"><h2>Informações do paciente</h2><button onClick={() => setEditing(true)}>Editar paciente</button></div><div className="clinic-actions"><button onClick={() => { setPhoto(null); setPhotoAction('upload') }}>{data.patient.fotoVersao ? 'Trocar foto' : 'Adicionar foto'}</button>{data.patient.fotoVersao && <button onClick={() => setPhotoAction('remove')}>Remover foto</button>}</div><dl className="clinic-details"><div><dt>Data de nascimento</dt><dd>{dateOnly(data.patient.dataNascimento)}</dd></div><div><dt>Peso</dt><dd>{data.patient.peso.toLocaleString('pt-BR')} kg</dd></div><div><dt>Identificação</dt><dd>#{data.patient.id}</dd></div></dl><h3>Observações</h3><p className="clinic-prewrap">{data.patient.observacoes || 'Nenhuma observação registrada.'}</p></section>
        <section className="clinic-panel"><div className="clinic-section-heading"><h2>Histórico de internações</h2><span className="clinic-badge">{data.history.length} registros</span></div>{data.history.length ? data.history.map(i => <article className="clinic-history" key={i.id}><StatusBadge status={i.status} /><h3>{i.motivo}</h3><p>Entrada: {dateTime(i.entradaInternacao)}{i.saidaInternacao && <> · Saída: {dateTime(i.saidaInternacao)}</>}</p><Link to={'/internacoes/' + i.id}>Ver internação #{i.id} →</Link></article>) : <EmptyState title="Uma nova história começa aqui"><p>Este paciente ainda não possui internações.</p></EmptyState>}</section>
      </div><aside><section className="clinic-panel"><span className="clinic-avatar"><UserRound size={22} /></span><div className="clinic-section-heading"><h2>Tutor responsável</h2><button onClick={() => setEditingTutor(true)}>Editar tutor</button></div>{data.tutor ? <><h3>{data.tutor.nome}</h3><dl className="clinic-details single"><div><dt>Telefone</dt><dd><a href={'tel:' + data.tutor.telefone.replace(/[^+\d]/g, '')}>{data.tutor.telefone}</a></dd></div><div><dt>E-mail</dt><dd><a href={'mailto:' + data.tutor.email}>{data.tutor.email}</a></dd></div><div><dt>Endereço</dt><dd>{data.tutor.endereco}</dd></div></dl><Link to={'/pacientes?tutor=' + data.tutor.id}>Ver pacientes deste tutor →</Link></> : <p>Tutor não encontrado.</p>}</section></aside></div>
      {editing && <PatientEditor patient={data.patient} tutors={data.tutors} onClose={() => setEditing(false)} onSaved={(_patient, _admit, message) => { setEditing(false); setWarning(message); setSuccess('Paciente atualizado.'); resource.reload() }} />}
      {editingTutor && data.tutor && <Editor title="Editar tutor" fields={tutorFields.map(f => ({ ...f, value: String(data.tutor![f.name as keyof Tutor] ?? '') }))} onCancel={() => setEditingTutor(false)} onSave={async values => { await clinicApi.save('/tutores/' + data.tutor!.id, values, 'PUT'); setSuccess('Tutor atualizado.'); resource.reload() }} />}
      {photoAction && <Editor title={photoAction === 'upload' ? 'Foto do paciente' : 'Remover foto do paciente'} description={photoAction === 'remove' ? 'A foto será removida do cadastro e do armazenamento local.' : undefined} fields={[]} afterFields={photoAction === 'upload' && <PhotoInput onChange={setPhoto} />} onCancel={() => setPhotoAction(null)} onSave={async () => {
        if (photoAction === 'upload') { if (!photo) throw new ApiError(400, 'Selecione uma foto.'); await uploadPhoto(data.patient.id, photo) }
        else await request('/pacientes/' + id + '/foto', { method: 'DELETE' })
        setWarning(''); setSuccess(photoAction === 'upload' ? 'Foto salva.' : 'Foto removida.'); resource.reload()
      }} />}
    </>}
  </Page>
}
