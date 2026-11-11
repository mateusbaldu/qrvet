import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import {
  ArrowRight,
  ClipboardList,
  Copy,
  Download,
  Mail,
  Plus,
  Printer,
  Search,
  Utensils,
} from 'lucide-react'
import { FeedingChecklist } from './Feeding'
import { useAuth } from '../auth/useAuth'
import { PatientAlerts } from '../components/PatientAlerts'
import { EvolutionNotes } from '../components/EvolutionNotes'
import { PatientAvatar } from '../components/PatientEditor'
import { ClinicTabs } from '../components/ClinicTabs'
import { queryPage, returnToList, useListNavigation, useListScroll } from '../hooks/useListNavigation'
import { useResource } from '../hooks/useResource'
import {
  Editor,
  EmptyState,
  Notice,
  Page,
  Pagination,
  ResourceState,
  StatusBadge,
  type Field,
} from '../components/ClinicUI'
import { readableError, request } from '../services/api'
import {
  allPages,
  clinicApi,
  dateTime,
  labels,
  paginate,
  searchText,
  type Alimentacao,
  type Baia,
  type Cuidado,
  type Internacao,
  type Jejum,
  type Medicacao,
  type Paciente,
  type QrCode,
  type Veterinario,
} from '../services/clinic'

function AdmissionEditor({ patientId, onClose }: { patientId: string; onClose: () => void }) {
  const { user } = useAuth()
  const navigate = useNavigate()
  const options = useResource(async () => {
    const [patients, bays, vets, admissions] = await Promise.all([
      allPages<Paciente>('/pacientes'),
      allPages<Baia>('/baias?status=DISPONIVEL'),
      request<Veterinario[]>('/internacoes/veterinarios'),
      allPages<Internacao>('/internacoes/ativas'),
    ])
    return {
      patients: patients.filter(p => !admissions.some(i => i.pacienteId === p.id)),
      bays,
      vets,
    }
  }, ['options'])
  const fields: Field[] = [
    {
      name: 'pacienteId',
      label: 'Paciente',
      type: 'number',
      value: patientId,
      options: (options.data?.patients ?? []).map(p => ({
        value: p.id,
        label: p.nome + ' · ' + p.especie + ' (#' + p.id + ')',
      })),
    },
    {
      name: 'baiaId',
      label: 'Baia disponível',
      type: 'number',
      options: (options.data?.bays ?? []).map(b => ({ value: b.id, label: b.identificacao })),
    },
    {
      name: 'veterinarioId',
      label: 'Veterinário responsável',
      type: 'number',
      value: user?.role === 'VETERINARIO' ? user.id : '',
      options: (options.data?.vets ?? []).map(v => ({ value: v.id, label: v.name })),
      wide: true,
    },
    { name: 'motivo', label: 'Motivo da internação', type: 'textarea' },
    { name: 'diagnosticoInicial', label: 'Diagnóstico inicial', maxLength: 255, wide: true },
    { name: 'observacoes', label: 'Observações', type: 'textarea', required: false },
  ]
  const ready =
    options.data &&
    options.data.patients.length > 0 &&
    options.data.bays.length > 0 &&
    options.data.vets.length > 0
  return (
    <>
      <ResourceState {...options} />
      {options.data && !ready && (
        <section className="clinic-panel">
          <h2>Antes de abrir a internação</h2>
          <p>
            É necessário ter um paciente sem internação ativa, uma baia disponível e um veterinário
            com convite aceito.
          </p>
          <div className="clinic-actions">
            <Link to="/pacientes?novo=1">Cadastrar paciente e tutor</Link>
            <Link to="/baias">Baias</Link>
            {user?.role === 'ADMIN' && <Link to="/equipe">Equipe</Link>}
            <button onClick={options.reload}>Atualizar opções</button>
            <button onClick={onClose}>Cancelar</button>
          </div>
        </section>
      )}
      {ready && (
        <Editor
          closeOnSave={false}
          title="Abrir internação"
          description="Selecione o paciente e a equipe responsável. A baia será ocupada ao confirmar."
          fields={fields}
          onCancel={onClose}
          submitLabel="Abrir internação"
          onSave={async data => {
            const result = await clinicApi.save<Internacao>('/internacoes', data)
            navigate('/internacoes/' + result.id)
          }}
        />
      )}
    </>
  )
}

export function Hospitalizations() {
  const { params, updateQuery, listLocation } = useListNavigation()
  const page = queryPage(params)
  const [creating, setCreating] = useState(params.has('paciente') || params.has('nova'))
  const search = params.get('busca') ?? ''
  const requestedStatus = params.get('situacao') ?? 'ATIVA'
  const status = requestedStatus === 'TODAS' ? '' : ['ATIVA', 'ALTA', 'OBITO'].includes(requestedStatus) ? requestedStatus : 'ATIVA'
  const navigate = useNavigate()
  const resource = useResource(
    () =>
      clinicApi.list<Internacao>('/internacoes', page, {
        q: search.trim() || undefined,
        status: status || undefined,
      }),
    [page, search, status]
  )
  const result = resource.data
  useListScroll(!resource.loading && !!result)
  const closeEditor = () => {
    setCreating(false)
    if (params.has('paciente') || params.has('nova')) updateQuery({ paciente: null, nova: null })
  }
  return (
    <Page
      title="Internações"
      help="Para abrir uma internação, é necessário um paciente sem internação ativa, uma baia disponível e um veterinário com convite aceito. Use os filtros para consultar altas e óbitos."
      description="Acompanhe os pacientes internados e consulte o histórico de internações."
      action={
        <button className="clinic-primary" onClick={() => setCreating(true)}>
          <Plus size={17} />
          Abrir internação
        </button>
      }
    >
      {creating && (
        <AdmissionEditor patientId={params.get('paciente') ?? ''} onClose={closeEditor} />
      )}
      <div className="clinic-toolbar clinic-panel">
        <label className="clinic-search">
          <span>Buscar internação</span>
          <span className="clinic-search-input">
            <Search size={18} />
            <input
              type="search"
              placeholder="Paciente, baia, veterinário ou código"
              value={search}
              onChange={e => {
                updateQuery({ busca: e.target.value, pagina: 0 })
              }}
            />
          </span>
        </label>
        <div className="clinic-filters" role="group" aria-label="Situação da internação">
          {[
            ['ATIVA', 'Ativas'],
            ['ALTA', 'Altas'],
            ['OBITO', 'Óbitos'],
            ['', 'Todas'],
          ].map(([value, label]) => (
            <button
              key={value}
              aria-pressed={status === value}
              className={status === value ? 'selected' : ''}
              onClick={() => {
                updateQuery({ situacao: value || 'TODAS', pagina: 0 })
              }}
            >
              {label}
            </button>
          ))}
        </div>
      </div>
      <ResourceState {...resource} />
      {result && (
        <>
          <div className="clinic-grid">
            {result.items.map(i => (
              <article className="clinic-card admission-card" key={i.id}>
                <div className="clinic-section-heading">
                  <StatusBadge status={i.status} />
                  <small>#{i.id}</small>
                </div>
                <Link className="admission-card-identity" to={'/internacoes/' + i.id} state={{ returnTo: listLocation }}>
                  <PatientAvatar patient={{ id: i.pacienteId, nome: i.pacienteNome, fotoVersao: i.fotoVersao ?? null }} photoPath={'/internacoes/' + i.id + '/foto'} />
                  <span><h2>{i.pacienteNome}</h2><small>Baia {i.baiaIdentificacao} · {i.veterinarioNome}</small></span>
                </Link>
                <p className="clinic-clamp">{i.motivo}</p>
                <p className="clinic-card-date">Entrada: {dateTime(i.entradaInternacao)}</p>
                <Link className="clinic-card-link" to={'/internacoes/' + i.id} state={{ returnTo: listLocation }}>
                  Acompanhar internação <ArrowRight size={16} />
                </Link>
              </article>
            ))}
          </div>
          {!result.total && (
            <EmptyState title="Nenhuma internação nesta seleção">
              <p>Altere os filtros ou abra uma nova internação.</p>
            </EmptyState>
          )}
          <Pagination {...result} onChange={page => { updateQuery({ pagina: page }); window.scrollTo({ top: 0, behavior: 'instant' }) }} />
        </>
      )}
      <details className="clinic-code-lookup">
        <summary>Consultar pelo código da internação</summary>
        <form
          className="clinic-toolbar"
          onSubmit={e => {
            e.preventDefault()
            navigate('/internacoes/' + new FormData(e.currentTarget).get('id'))
          }}
        >
          <label>
            Código da internação
            <input name="id" type="number" min="1" step="1" required />
          </label>
          <button>Consultar</button>
        </form>
      </details>
    </Page>
  )
}

export function HospitalizationDetail() {
  const { id = '' } = useParams()
  const location = useLocation()
  const { params, updateQuery } = useListNavigation()
  const { user } = useAuth()
  const clinical = user?.role === 'ADMIN' || user?.role === 'VETERINARIO'
  const tab = ['evolucoes', 'cuidados'].includes(params.get('aba') ?? '') ? params.get('aba')! : 'resumo'
  const [closing, setClosing] = useState(false)
  const [success, setSuccess] = useState('')
  const resource = useResource(
    () => request<Internacao>('/internacoes/' + encodeURIComponent(id)),
    [id]
  )
  const i = resource.data
  return (
    <Page
      title={i?.pacienteNome ?? 'Internação'}
      help="O QR Code da etiqueta abre esta internação para a equipe autenticada. Para o tutor, use o envio por e-mail: ele recebe um link de acompanhamento sem login."
      description={
        i
          ? 'Internação #' + i.id + ' · Baia ' + i.baiaIdentificacao + ' · ' + i.veterinarioNome
          : 'Acompanhamento e histórico de cuidados.'
      }
      action={
        <Link className="clinic-button" to={returnToList(location.state, '/internacoes')}>
          Voltar às internações
        </Link>
      }
    >
      {success && <Notice>{success}</Notice>}
      <ResourceState {...resource} />
      {i && (
        <>
          <div className="clinic-patient-banner hospitalization-banner">
            <PatientAvatar patient={{ id: i.pacienteId, nome: i.pacienteNome, fotoVersao: i.fotoVersao ?? null }} photoPath={'/internacoes/' + i.id + '/foto'} large />
            <div>
              <strong className="hospitalization-patient-name">{i.pacienteNome}</strong>
              <StatusBadge status={i.status}>
                {labels[i.status]} · #{i.id}
              </StatusBadge>
              <p>
                Entrada em {dateTime(i.entradaInternacao)}
                {i.saidaInternacao && ' · Saída em ' + dateTime(i.saidaInternacao)}
              </p>
            </div>
            <div className="clinic-actions">
              <Link to={'/pacientes/' + i.pacienteId}>Ver paciente</Link>
              {i.status === 'ATIVA' && (
                <button onClick={() => setClosing(true)}>Encerrar internação</button>
              )}
            </div>
          </div>
          <PatientAlerts alerts={i.alertas} patientId={i.pacienteId} onSaved={resource.reload} />
          <ClinicTabs label="Seções da internação" value={clinical ? tab : 'resumo'} onChange={aba => updateQuery({ aba })} tabs={[
            { id: 'resumo', label: 'Resumo', content: <div className="clinic-detail-grid">
              <section className="clinic-panel">
                <h2>Resumo clínico</h2>
                <dl className="clinic-details single">
                  {Object.entries({
                    'Motivo': i.motivo,
                    'Diagnóstico inicial': i.diagnosticoInicial,
                    'Veterinário responsável': i.veterinarioNome,
                    'Entrada': dateTime(i.entradaInternacao),
                    ...(i.saidaInternacao ? { 'Saída': dateTime(i.saidaInternacao) } : {}),
                    'Observações': i.observacoes || 'Nenhuma observação registrada.',
                  }).map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}
                </dl>
              </section>
              <aside><QrPanel id={id} patientName={i.pacienteNome} /></aside>
            </div> },
            ...(clinical ? [
              { id: 'evolucoes', label: 'Evoluções', content: <EvolutionNotes admissionId={id} active={i.status === 'ATIVA'} /> },
              { id: 'cuidados', label: 'Cuidados', content: <CarePanel id={id} admission={i} /> },
            ] : []),
          ]} />
          {closing && (
            <Editor
              title="Encerrar internação"
              description="O histórico será preservado. A baia será liberada, o jejum será encerrado e as refeições pendentes serão canceladas. Confira o desfecho antes de confirmar."
              submitLabel="Confirmar encerramento"
              fields={[
                {
                  name: 'statusEncerramento',
                  label: 'Desfecho',
                  options: [
                    { value: 'ALTA', label: 'Alta' },
                    { value: 'OBITO', label: 'Óbito' },
                  ],
                },
              ]}
              onCancel={() => setClosing(false)}
              onSave={async data => {
                await clinicApi.save('/internacoes/' + id + '/encerrar', data, 'PUT')
                setSuccess('Internação encerrada. A baia está disponível novamente.')
                resource.reload()
              }}
            />
          )}
        </>
      )}
    </Page>
  )
}

export function QrPanel({ id, patientName }: { id: string; patientName: string }) {
  const resource = useResource(() => request<QrCode>('/internacoes/' + id + '/qrcode'), [id])
  const [message, setMessage] = useState('')
  const [sending, setSending] = useState(false)
  const [emailMessage, setEmailMessage] = useState('')
  const [emailError, setEmailError] = useState('')
  async function sendEmail() {
    if (sending) return
    setSending(true)
    setEmailMessage('')
    setEmailError('')
    try {
      const result = await request<{ message: string }>('/internacoes/' + id + '/tutor/email', {
        method: 'POST',
      })
      setEmailMessage(result.message)
    } catch (error) {
      setEmailError(readableError(error))
    } finally {
      setSending(false)
    }
  }
  return (
    <>
      <section className="clinic-panel clinic-qr-panel">
        <h2>QR Code da internação</h2>
        <p>
          Imprima e cole no vidro da clínica. Ao escanear, a equipe abre esta internação com seu
          acesso ao sistema.
        </p>
        <ResourceState {...resource} />
        {resource.data && (
          <>
            <div className="clinic-print-label">
              <strong>
                {patientName} · Internação #{id}
              </strong>
              <img
                className="clinic-qr"
                src={'data:image/png;base64,' + resource.data.base64}
                alt={'QR Code da internação de ' + patientName}
              />
            </div>
            <div className="clinic-actions">
              <button
                onClick={async () => {
                  try {
                    await navigator.clipboard.writeText(resource.data!.url)
                    setMessage('Link copiado.')
                  } catch {
                    setMessage('Selecione e copie o endereço abaixo.')
                  }
                }}
              >
                <Copy size={15} />
                Copiar link
              </button>
              <a
                className="clinic-button"
                href={'data:image/png;base64,' + resource.data.base64}
                download={'internacao-' + id + '.png'}
              >
                <Download size={15} />
                Baixar QR
              </a>
              <button onClick={() => window.print()}>
                <Printer size={15} />
                Imprimir
              </button>
            </div>
            {message && (
              <p role="status" className="clinic-copy-message">
                {message}
              </p>
            )}
            <label className="clinic-qr-url">
              Link da internação (equipe)
              <input readOnly value={resource.data.url} onFocus={e => e.target.select()} />
            </label>
          </>
        )}
      </section>
      {resource.data && (
        <section className="clinic-panel">
          <h2>Acesso do tutor</h2>
          <p>
            Envie o link de acompanhamento para <strong>{resource.data.tutorEmail}</strong>. O tutor
            pode consultar sem login.
          </p>
          <div className="clinic-actions">
            <button disabled={sending} onClick={sendEmail}>
              <Mail size={15} />
              {sending ? 'Enviando...' : 'Enviar link por e-mail'}
            </button>
          </div>
          {emailMessage && (
            <p role="status" className="clinic-copy-message">
              {emailMessage}
            </p>
          )}
          {emailError && (
            <p role="alert" className="clinic-error">
              {emailError}
            </p>
          )}
          <label className="clinic-qr-url">
            Link de acompanhamento do tutor
            <input readOnly value={resource.data.tutorUrl} onFocus={e => e.target.select()} />
          </label>
          <Link to={'/public/internacoes/qr/' + resource.data.uuidToken}>
            Abrir consulta pública →
          </Link>
        </section>
      )}
    </>
  )
}

function careFieldsForAction(action: 'food' | 'medication' | 'fast' | 'end' | null): Field[] {
  if (action === 'fast') {
    return [{ name: 'motivo', label: 'Motivo do jejum', maxLength: 255, wide: true }]
  }
  if (action === 'food') {
    return [
      { name: 'alimento', label: 'Alimento', maxLength: 100 },
      { name: 'quantidade', label: 'Quantidade (inclua a unidade)', maxLength: 50 },
      {
        name: 'aceitacaoObservacao',
        label: 'Aceitação e observações',
        type: 'textarea',
        required: false,
      },
    ]
  }
  if (action === 'medication') {
    return [
      { name: 'medicamento', label: 'Medicamento', maxLength: 100 },
      { name: 'dose', label: 'Dose', maxLength: 50 },
      { name: 'unidade', label: 'Unidade', maxLength: 30, hint: 'Ex.: mg/kg, mL, UI' },
      {
        name: 'via',
        label: 'Via',
        maxLength: 50,
        hint: 'Ex.: Oral, Intravenosa, Subcutânea',
      },
      { name: 'prescricao', label: 'Prescrição', maxLength: 255, wide: true },
      {
        name: 'observacoes',
        label: 'Observações',
        type: 'textarea',
        required: false,
      },
    ]
  }
  return []
}

function careEditorTitle(action: 'food' | 'medication' | 'fast' | 'end'): string {
  if (action === 'food') return 'Registrar alimentação'
  if (action === 'medication') return 'Registrar medicação'
  if (action === 'fast') return 'Iniciar jejum'
  return 'Confirmar encerramento do jejum'
}

function careEditorDescription(
  action: 'food' | 'medication' | 'fast' | 'end'
): string | undefined {
  if (action === 'end') {
    return 'Confirme o encerramento para liberar os registros de alimentação.'
  }
  if (action === 'medication') {
    return 'Confirme a administração da medicação ao paciente.'
  }
  return undefined
}

function careEndpointSuffix(action: 'food' | 'medication' | 'fast' | 'end'): string {
  if (action === 'food') return 'alimentacao'
  if (action === 'medication') return 'medicacao'
  if (action === 'fast') return 'jejum'
  return 'jejum/encerrar'
}

function careSuccessMessage(action: 'food' | 'medication' | 'fast' | 'end'): string {
  if (action === 'food') return 'Alimentação registrada.'
  if (action === 'medication') return 'Medicação registrada.'
  if (action === 'fast') return 'Jejum iniciado.'
  return 'Jejum encerrado.'
}

export function CarePanel({ id, admission }: { id: string; admission?: Internacao }) {
  const [action, setAction] = useState<'food' | 'medication' | 'fast' | 'end' | null>(null)
  const [success, setSuccess] = useState('')
  const resource = useResource(
    async () => {
      const [summary, fasts, foods, medications] = await Promise.all([
        request<Cuidado>('/internacoes/' + id + '/cuidados'),
        request<Jejum[]>('/internacoes/' + id + '/jejum'),
        request<Alimentacao[]>('/internacoes/' + id + '/alimentacao'),
        request<Medicacao[]>('/internacoes/' + id + '/medicacao'),
      ])
      return { summary, fasts, foods, medications }
    },
    [id, admission?.status]
  )
  const fasting = resource.data?.fasts.some(f => f.ativo) ?? false
  const fields = careFieldsForAction(action)
  const events = resource.data
    ? [
        ...resource.data.foods.map(f => ({
          key: 'food' + f.id,
          date: f.dataHoraRegistro,
          title: 'Alimentação registrada',
          detail: f.alimento + ' · ' + f.quantidade,
          note: f.aceitacaoObservacao,
          tone: 'food',
        })),
        ...resource.data.medications.map(m => ({
          key: 'medication' + m.id,
          date: m.dataHoraRegistro,
          title: 'Medicação registrada',
          detail: m.medicamento + ' · ' + m.dose + ' ' + m.unidade + ' · ' + m.via,
          note: [m.prescricao, m.observacoes, m.responsavelNome].filter(Boolean).join(' · '),
          tone: 'medication',
        })),
        ...resource.data.fasts.flatMap(f => [
          {
            key: 'fast' + f.id,
            date: f.dataHoraInicio,
            title: 'Jejum iniciado',
            detail: f.motivo,
            note: '',
            tone: 'fast',
          },
          ...(f.dataHoraFim
            ? [
                {
                  key: 'end' + f.id,
                  date: f.dataHoraFim,
                  title: 'Jejum encerrado',
                  detail: f.motivo,
                  note: '',
                  tone: 'normal',
                },
              ]
            : []),
        ]),
        ...(admission
          ? [
              {
                key: 'entry',
                date: admission.entradaInternacao,
                title: 'Internação iniciada',
                detail: admission.motivo,
                note: admission.veterinarioNome,
                tone: 'normal',
              },
              ...(admission.saidaInternacao
                ? [
                    {
                      key: 'discharge',
                      date: admission.saidaInternacao,
                      title: 'Internação encerrada · ' + labels[admission.status],
                      detail: 'Baia liberada',
                      note: '',
                      tone: 'normal',
                    },
                  ]
                : []),
            ]
          : []),
      ].sort((a, b) => Date.parse(b.date) - Date.parse(a.date))
    : []
  const { params, updateQuery } = useListNavigation()
  const careTab = params.get('aba') === 'evolucoes' ? 'evolucoes' : 'cuidados'
  const careContent = <>
      <FeedingChecklist
        admissionId={id}
        admissionSummary={resource.data?.summary}
        active={resource.data?.summary.status === 'ATIVA'}
        onChanged={resource.reload}
      />
      <section className="clinic-panel">
        <div className="clinic-section-heading">
          <div>
            <h2>Alimentação e jejum</h2>
            <p>Registros de cuidado durante a internação.</p>
          </div>
          <Utensils size={21} />
        </div>
        {success && <Notice>{success}</Notice>}
        <ResourceState {...resource} />
        {resource.data && (
          <>
            {!admission && (
              <p>
                <strong>{resource.data.summary.pacienteNome}</strong> · Baia{' '}
                {resource.data.summary.baiaIdentificacao} · {labels[resource.data.summary.status]}
              </p>
            )}
            {resource.data.summary.status !== 'ATIVA' ? (
              <p className="clinic-notice">
                Internação encerrada. Histórico disponível para consulta.
              </p>
            ) : (
              <>
                {fasting && (
                  <p className="clinic-notice" role="status">
                    Jejum ativo. Não oferecer alimento até o encerramento do jejum.
                  </p>
                )}
                <div className="clinic-actions">
                  <button
                    className="clinic-primary"
                    disabled={fasting}
                    onClick={() => setAction('food')}
                  >
                    Registrar alimentação
                  </button>
                  <button onClick={() => setAction('medication')}>
                    Registrar medicação
                  </button>
                  <button onClick={() => setAction(fasting ? 'end' : 'fast')}>
                    {fasting ? 'Encerrar jejum' : 'Iniciar jejum'}
                  </button>
                </div>
              </>
            )}
            {action && (
              <Editor
                title={careEditorTitle(action)}
                description={careEditorDescription(action)}
                fields={fields}
                onCancel={() => setAction(null)}
                onSave={async data => {
                  let method = 'POST'
                  let payload: Record<string, string | number> | undefined = data
                  if (action === 'end') {
                    method = 'PUT'
                    payload = undefined
                  }
                  await clinicApi.save(
                    '/internacoes/' + id + '/' + careEndpointSuffix(action),
                    payload,
                    method
                  )
                  setSuccess(careSuccessMessage(action))
                  resource.reload()
                }}
              />
            )}
            <h3 className="clinic-timeline-title">
              <ClipboardList size={18} />
              Linha do tempo
            </h3>
            {events.length ? (
              <ol className="clinic-timeline">
                {events.map(event => (
                  <li key={event.key} className={'event-' + event.tone}>
                    <time>{dateTime(event.date)}</time>
                    <h3>{event.title}</h3>
                    <p>{event.detail}</p>
                    {event.note && <small>{event.note}</small>}
                  </li>
                ))}
              </ol>
            ) : (
              <EmptyState title="Nenhum cuidado registrado">
                <p>Os registros de alimentação e jejum aparecerão aqui.</p>
              </EmptyState>
            )}
          </>
        )}
      </section>
  </>
  return <>
    {!admission && resource.data && <>
      <div className="clinic-patient-banner hospitalization-banner">
        <PatientAvatar patient={{ id: resource.data.summary.id, nome: resource.data.summary.pacienteNome, fotoVersao: resource.data.summary.fotoVersao ?? null }} photoPath={'/internacoes/' + id + '/foto'} large />
        <div><strong className="hospitalization-patient-name">{resource.data.summary.pacienteNome}</strong><p>Baia {resource.data.summary.baiaIdentificacao} · Internação #{id}</p><StatusBadge status={resource.data.summary.status} /></div>
      </div>
      <PatientAlerts alerts={resource.data.summary.alertas} />
    </>}
    {!admission ? <ClinicTabs label="Seções dos cuidados" value={careTab} onChange={aba => updateQuery({ aba })} tabs={[
      { id: 'cuidados', label: 'Cuidados', content: careContent },
      { id: 'evolucoes', label: 'Evoluções', content: resource.data && <EvolutionNotes admissionId={id} active={resource.data.summary.status === 'ATIVA'} /> },
    ]} /> : careContent}
  </>
}

export function CareLookup() {
  const { params, updateQuery, listLocation } = useListNavigation()
  const id = params.get('internacao') ?? ''
  const search = params.get('busca') ?? ''
  const resource = useResource(() => allPages<Cuidado>('/internacoes/cuidados'), ['care'])
  const matches = (resource.data ?? []).filter(i => searchText(`${i.pacienteNome} ${i.baiaIdentificacao} ${i.id}`).includes(searchText(search.trim())))
  const result = paginate(matches, queryPage(params))
  useListScroll(!resource.loading && !!resource.data, !id)
  const listParams = new URLSearchParams()
  for (const key of ['busca', 'pagina']) if (params.has(key)) listParams.set(key, params.get(key)!)
  const returnUrl = '/cuidados' + (listParams.size ? '?' + listParams.toString() : '')
  return <Page title="Cuidados" description="Encontre o paciente pela foto, pelo nome ou pela baia e abra os cuidados."
    help="Selecione o cartão de um paciente internado para registrar os cuidados e consultar as evoluções. Para internações encerradas, consulte pelo código."
    action={<Link className="clinic-button clinic-primary" to="/alimentacao">Checklist geral de alimentação</Link>}>
    {id ? <>
      <Link className="clinic-button care-return-link" to={returnUrl}>Voltar aos pacientes em cuidados</Link>
      <CarePanel key={id} id={id} />
    </> : <>
      <div className="clinic-toolbar clinic-panel"><label className="clinic-search"><span>Buscar paciente ou baia</span><span className="clinic-search-input"><Search size={18} /><input type="search" placeholder="Nome, baia ou código da internação" value={search} onChange={e => updateQuery({ busca: e.target.value, pagina: 0 })} /></span></label></div>
      <ResourceState {...resource} />
      {resource.data && <>
        <div className="care-directory-heading"><p>{matches.length} paciente(s) nesta seleção</p></div>
        {result.items.length ? <div className="care-patient-grid">{result.items.map(i => {
          const target = new URLSearchParams(params)
          target.set('internacao', String(i.id))
          target.delete('aba')
          return <Link className="care-patient-card" key={i.id} to={'/cuidados?' + target.toString()} state={{ returnTo: listLocation }} aria-label={'Abrir cuidados de ' + i.pacienteNome}>
            <PatientAvatar patient={{ id: i.id, nome: i.pacienteNome, fotoVersao: i.fotoVersao ?? null }} photoPath={'/internacoes/' + i.id + '/foto'} />
            <span><strong>{i.pacienteNome}</strong><small>Baia {i.baiaIdentificacao} · #{i.id}</small><StatusBadge status={i.status} /></span><ArrowRight size={18} />
          </Link>
        })}</div> : <EmptyState title={resource.data.length ? 'Nenhum paciente nesta busca' : 'Nenhuma internação ativa'}><p>{resource.data.length ? 'Tente outro nome, baia ou código.' : 'Novas internações aparecerão aqui para acompanhamento.'}</p></EmptyState>}
        {result.pages > 1 && <Pagination {...result} onChange={pagina => { updateQuery({ pagina }); window.scrollTo({ top: 0, behavior: 'instant' }) }} />}
      </>}
      <details className="clinic-code-lookup"><summary>Consultar histórico pelo código</summary><form className="clinic-toolbar" onSubmit={e => {
        e.preventDefault()
        updateQuery({ internacao: String(new FormData(e.currentTarget).get('id')), aba: null }, false)
      }}><label>Código da internação<input name="id" type="number" min="1" step="1" required /></label><button>Consultar</button></form></details>
    </>}
  </Page>
}
