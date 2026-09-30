import { useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ArrowRight, ClipboardList, Copy, Download, HeartPulse, Plus, Printer, Search, Utensils } from 'lucide-react'
import { useAuth } from '../auth/useAuth'
import { useResource } from '../hooks/useResource'
import { Editor, EmptyState, Notice, Page, Pagination, ResourceState, StatusBadge, type Field } from '../components/ClinicUI'
import { request } from '../services/api'
import { allPages, clinicApi, dateTime, labels, paginate, searchText, type Alimentacao, type Baia, type Cuidado, type Internacao, type Jejum, type Paciente, type QrCode, type Veterinario } from '../services/clinic'

function AdmissionEditor({ patientId, onClose }: { patientId: string; onClose: () => void }) {
  const { user } = useAuth()
  const navigate = useNavigate()
  const options = useResource(async () => {
    const [patients, bays, vets, admissions] = await Promise.all([allPages<Paciente>('/pacientes'), allPages<Baia>('/baias?status=DISPONIVEL'), request<Veterinario[]>('/internacoes/veterinarios'), allPages<Internacao>('/internacoes/ativas')])
    return { patients: patients.filter(p => !admissions.some(i => i.pacienteId === p.id)), bays, vets }
  }, 'options')
  const fields: Field[] = [
    { name: 'pacienteId', label: 'Paciente', type: 'number', value: patientId, options: (options.data?.patients ?? []).map(p => ({ value: p.id, label: p.nome + ' · ' + p.especie + ' (#' + p.id + ')' })) },
    { name: 'baiaId', label: 'Baia disponível', type: 'number', options: (options.data?.bays ?? []).map(b => ({ value: b.id, label: b.identificacao })) },
    { name: 'veterinarioId', label: 'Veterinário responsável', type: 'number', value: user?.role === 'VETERINARIO' ? user.id : '', options: (options.data?.vets ?? []).map(v => ({ value: v.id, label: v.name })), wide: true },
    { name: 'motivo', label: 'Motivo da internação', type: 'textarea' },
    { name: 'diagnosticoInicial', label: 'Diagnóstico inicial', maxLength: 255, wide: true },
    { name: 'observacoes', label: 'Observações', type: 'textarea', required: false },
  ]
  const ready = options.data && options.data.patients.length > 0 && options.data.bays.length > 0 && options.data.vets.length > 0
  return <><ResourceState {...options} />{options.data && !ready && <section className="clinic-panel"><h2>Antes de abrir a internação</h2><p>É necessário ter um paciente sem internação ativa, uma baia disponível e um veterinário com convite aceito.</p><div className="clinic-actions"><Link to="/pacientes">Pacientes</Link><Link to="/baias">Baias</Link>{user?.role === 'ADMIN' && <Link to="/equipe">Equipe</Link>}<button onClick={options.reload}>Atualizar opções</button><button onClick={onClose}>Cancelar</button></div></section>}{ready && <Editor closeOnSave={false} title="Abrir internação" description="Selecione o paciente e a equipe responsável. A baia será ocupada ao confirmar." fields={fields} onCancel={onClose} submitLabel="Abrir internação" onSave={async data => { const result = await clinicApi.save<Internacao>('/internacoes', data); navigate('/internacoes/' + result.id) }} />}</>
}

export function Hospitalizations() {
  const [params, setParams] = useSearchParams()
  const [page, setPage] = useState(0)
  const [creating, setCreating] = useState(params.has('paciente') || params.has('nova'))
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('ATIVA')
  const navigate = useNavigate()
  const resource = useResource(() => allPages<Internacao>('/internacoes'), 'admissions')
  const filtered = (resource.data ?? []).filter(i => (!status || i.status === status) && searchText([i.pacienteNome, i.baiaIdentificacao, i.veterinarioNome, i.id].join(' ')).includes(searchText(search.trim())))
  const result = paginate(filtered, page)
  const closeEditor = () => { setCreating(false); if (params.has('paciente') || params.has('nova')) setParams({}, { replace: true }) }
  return <Page title="Internações" description="Um olhar atento para cada paciente, da entrada à recuperação." action={<button className="clinic-primary" onClick={() => setCreating(true)}><Plus size={17} />Abrir internação</button>}>
    {creating && <AdmissionEditor patientId={params.get('paciente') ?? ''} onClose={closeEditor} />}
    <div className="clinic-toolbar clinic-panel"><label className="clinic-search"><span>Buscar internação</span><span className="clinic-search-input"><Search size={18} /><input type="search" placeholder="Paciente, baia, veterinário ou código" value={search} onChange={e => { setSearch(e.target.value); setPage(0) }} /></span></label><div className="clinic-filters" role="group" aria-label="Situação da internação">{[['ATIVA', 'Ativas'], ['ALTA', 'Altas'], ['OBITO', 'Óbitos'], ['', 'Todas']].map(([value, label]) => <button key={value} aria-pressed={status === value} className={status === value ? 'selected' : ''} onClick={() => { setStatus(value); setPage(0) }}>{label}</button>)}</div></div>
    <ResourceState {...resource} />
    {resource.data && <><div className="clinic-grid">{result.items.map(i => <article className="clinic-card admission-card" key={i.id}><div className="clinic-section-heading"><StatusBadge status={i.status} /><small>#{i.id}</small></div><h2>{i.pacienteNome}</h2><p className="clinic-card-location">Baia {i.baiaIdentificacao} · {i.veterinarioNome}</p><p className="clinic-clamp">{i.motivo}</p><p className="clinic-card-date">Entrada: {dateTime(i.entradaInternacao)}</p><Link className="clinic-card-link" to={'/internacoes/' + i.id}>Acompanhar internação <ArrowRight size={16} /></Link></article>)}</div>{!result.total && <EmptyState title="Nenhuma internação nesta seleção"><p>Altere os filtros ou abra uma nova internação.</p></EmptyState>}<Pagination {...result} onChange={setPage} /></>}
    <details className="clinic-code-lookup"><summary>Consultar pelo código da internação</summary><form className="clinic-toolbar" onSubmit={e => { e.preventDefault(); navigate('/internacoes/' + new FormData(e.currentTarget).get('id')) }}><label>Código da internação<input name="id" type="number" min="1" step="1" required /></label><button>Consultar</button></form></details>
  </Page>
}

export function HospitalizationDetail() {
  const { id = '' } = useParams()
  const { user } = useAuth()
  const [closing, setClosing] = useState(false)
  const [success, setSuccess] = useState('')
  const resource = useResource(() => request<Internacao>('/internacoes/' + encodeURIComponent(id)), id)
  const i = resource.data
  return <Page title={i?.pacienteNome ?? 'Internação'} description={i ? 'Internação #' + i.id + ' · Baia ' + i.baiaIdentificacao + ' · ' + i.veterinarioNome : 'Acompanhamento e histórico de cuidados.'} action={<Link className="clinic-button" to="/internacoes">Voltar às internações</Link>}>
    {success && <Notice>{success}</Notice>}<ResourceState {...resource} />
    {i && <><div className="clinic-patient-banner"><span className="clinic-avatar"><HeartPulse size={23} /></span><div><StatusBadge status={i.status}>{labels[i.status]} · #{i.id}</StatusBadge><p>Entrada em {dateTime(i.entradaInternacao)}{i.saidaInternacao && ' · Saída em ' + dateTime(i.saidaInternacao)}</p></div><div className="clinic-actions"><Link to={'/pacientes/' + i.pacienteId}>Ver paciente</Link>{i.status === 'ATIVA' && <button onClick={() => setClosing(true)}>Encerrar internação</button>}</div></div>
      <div className="clinic-detail-grid"><div>{(user?.role === 'ADMIN' || user?.role === 'VETERINARIO') ? <CarePanel id={id} admission={i} /> : <section className="clinic-panel"><h2>Informações da internação</h2><p>{i.motivo}</p><p>Os cuidados são registrados pela equipe clínica.</p></section>}</div><aside><QrPanel id={id} patientName={i.pacienteNome} /><section className="clinic-panel"><h2>Resumo clínico</h2><dl className="clinic-details single">{Object.entries({ 'Motivo': i.motivo, 'Diagnóstico inicial': i.diagnosticoInicial, 'Veterinário responsável': i.veterinarioNome, 'Observações': i.observacoes || 'Nenhuma observação registrada.' }).map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl></section></aside></div>
      {closing && <Editor title="Encerrar internação" description="O histórico será preservado. A baia será liberada e um eventual jejum ativo será encerrado. Confira o desfecho antes de confirmar." submitLabel="Confirmar encerramento" fields={[{ name: 'statusEncerramento', label: 'Desfecho', options: [{ value: 'ALTA', label: 'Alta' }, { value: 'OBITO', label: 'Óbito' }] }]} onCancel={() => setClosing(false)} onSave={async data => { await clinicApi.save('/internacoes/' + id + '/encerrar', data, 'PUT'); setSuccess('Internação encerrada. A baia está disponível novamente.'); resource.reload() }} />}
    </>}
  </Page>
}

export function QrPanel({ id, patientName }: { id: string; patientName: string }) {
  const resource = useResource(() => request<QrCode>('/internacoes/' + id + '/qrcode'), id)
  const [message, setMessage] = useState('')
  return <section className="clinic-panel clinic-qr-panel"><h2>Acesso do tutor</h2><p>Compartilhe o acompanhamento da internação.</p><ResourceState {...resource} />{resource.data && <>
    <div className="clinic-print-label"><strong>{patientName} · Internação #{id}</strong><img className="clinic-qr" src={'data:image/png;base64,' + resource.data.base64} alt={'QR Code da internação de ' + patientName} /></div>
    <div className="clinic-actions"><button onClick={async () => { try { await navigator.clipboard.writeText(resource.data!.url); setMessage('Link copiado.') } catch { setMessage('Selecione e copie o endereço abaixo.') } }}><Copy size={15} />Copiar link</button><a className="clinic-button" href={'data:image/png;base64,' + resource.data.base64} download={'internacao-' + id + '.png'}><Download size={15} />Baixar QR</a><button onClick={() => window.print()}><Printer size={15} />Imprimir</button></div>
    {message && <p role="status" className="clinic-copy-message">{message}</p>}<label className="clinic-qr-url">Link de acompanhamento<input readOnly value={resource.data.url} onFocus={e => e.target.select()} /></label><Link to={'/public/internacoes/qr/' + resource.data.uuidToken}>Abrir consulta pública →</Link>
  </>}</section>
}

export function CarePanel({ id, admission }: { id: string; admission?: Internacao }) {
  const [action, setAction] = useState<'food' | 'fast' | 'end' | null>(null)
  const [success, setSuccess] = useState('')
  const resource = useResource(async () => {
    const [summary, fasts, foods] = await Promise.all([request<Cuidado>('/internacoes/' + id + '/cuidados'), request<Jejum[]>('/internacoes/' + id + '/jejum'), request<Alimentacao[]>('/internacoes/' + id + '/alimentacao')])
    return { summary, fasts, foods }
  }, id + ':' + (admission?.status ?? ''))
  const fasting = resource.data?.fasts.some(f => f.ativo) ?? false
  const fields: Field[] = action === 'fast' ? [{ name: 'motivo', label: 'Motivo do jejum', maxLength: 255, wide: true }] : action === 'food' ? [{ name: 'alimento', label: 'Alimento', maxLength: 100 }, { name: 'quantidade', label: 'Quantidade (inclua a unidade)', maxLength: 50 }, { name: 'aceitacaoObservacao', label: 'Aceitação e observações', type: 'textarea', required: false }] : []
  const events = resource.data ? [
    ...resource.data.foods.map(f => ({ key: 'food' + f.id, date: f.dataHoraRegistro, title: 'Alimentação registrada', detail: f.alimento + ' · ' + f.quantidade, note: f.aceitacaoObservacao, tone: 'food' })),
    ...resource.data.fasts.flatMap(f => [{ key: 'fast' + f.id, date: f.dataHoraInicio, title: 'Jejum iniciado', detail: f.motivo, note: '', tone: 'fast' }, ...(f.dataHoraFim ? [{ key: 'end' + f.id, date: f.dataHoraFim, title: 'Jejum encerrado', detail: f.motivo, note: '', tone: 'normal' }] : [])]),
    ...(admission ? [{ key: 'entry', date: admission.entradaInternacao, title: 'Internação iniciada', detail: admission.motivo, note: admission.veterinarioNome, tone: 'normal' }, ...(admission.saidaInternacao ? [{ key: 'discharge', date: admission.saidaInternacao, title: 'Internação encerrada · ' + labels[admission.status], detail: 'Baia liberada', note: '', tone: 'normal' }] : [])] : []),
  ].sort((a, b) => Date.parse(b.date) - Date.parse(a.date)) : []
  return <section className="clinic-panel"><div className="clinic-section-heading"><div><h2>Alimentação e jejum</h2><p>Registros de cuidado durante a internação.</p></div><Utensils size={21} /></div>
    {success && <Notice>{success}</Notice>}<ResourceState {...resource} />
    {resource.data && <>{!admission && <p><strong>{resource.data.summary.pacienteNome}</strong> · Baia {resource.data.summary.baiaIdentificacao} · {labels[resource.data.summary.status]}</p>}
      {resource.data.summary.status !== 'ATIVA' ? <p className="clinic-notice">Internação encerrada. Histórico disponível para consulta.</p> : <>{fasting && <p className="clinic-notice" role="status">Jejum ativo. Não oferecer alimento até o encerramento do jejum.</p>}<div className="clinic-actions"><button className="clinic-primary" disabled={fasting} onClick={() => setAction('food')}>Registrar alimentação</button><button onClick={() => setAction(fasting ? 'end' : 'fast')}>{fasting ? 'Encerrar jejum' : 'Iniciar jejum'}</button></div></>}
      {action && <Editor title={action === 'food' ? 'Registrar alimentação' : action === 'fast' ? 'Iniciar jejum' : 'Confirmar encerramento do jejum'} description={action === 'end' ? 'Confirme o encerramento para liberar os registros de alimentação.' : undefined} fields={fields} onCancel={() => setAction(null)} onSave={async data => { await clinicApi.save('/internacoes/' + id + '/' + (action === 'food' ? 'alimentacao' : action === 'fast' ? 'jejum' : 'jejum/encerrar'), action === 'end' ? undefined : data, action === 'end' ? 'PUT' : 'POST'); setSuccess(action === 'food' ? 'Alimentação registrada.' : action === 'fast' ? 'Jejum iniciado.' : 'Jejum encerrado.'); resource.reload() }} />}
      <h3 className="clinic-timeline-title"><ClipboardList size={18} />Linha do tempo</h3>
      {events.length ? <ol className="clinic-timeline">{events.map(event => <li key={event.key} className={'event-' + event.tone}><time>{dateTime(event.date)}</time><h3>{event.title}</h3><p>{event.detail}</p>{event.note && <small>{event.note}</small>}</li>)}</ol> : <EmptyState title="Nenhum cuidado registrado"><p>Os registros de alimentação e jejum aparecerão aqui.</p></EmptyState>}
    </>}
  </section>
}

export function CareLookup() {
  const [id, setId] = useState('')
  const resource = useResource(() => allPages<Cuidado>('/internacoes/cuidados'), 'care')
  return <Page title="Cuidados" description="Acompanhe a alimentação e o jejum dos pacientes internados.">
    <ResourceState {...resource} />
    <div className="clinic-toolbar clinic-panel"><label className="clinic-search">Paciente internado<select value={resource.data?.some(i => String(i.id) === id) ? id : ''} onChange={e => setId(e.target.value)}><option value="">Selecione um paciente</option>{resource.data?.map(i => <option key={i.id} value={i.id}>{i.pacienteNome} · Baia {i.baiaIdentificacao} · #{i.id}</option>)}</select></label></div>
    <details className="clinic-code-lookup"><summary>Consultar histórico pelo código</summary><form className="clinic-toolbar" onSubmit={e => { e.preventDefault(); setId(String(new FormData(e.currentTarget).get('id'))) }}><label>Código da internação<input name="id" type="number" min="1" step="1" required /></label><button>Consultar</button></form></details>
    {id ? <CarePanel key={id} id={id} /> : <EmptyState title={resource.data?.length ? 'Selecione um paciente para começar' : 'Nenhuma internação ativa'}><p>Consulte o histórico ou registre os cuidados da equipe.</p></EmptyState>}
  </Page>
}


