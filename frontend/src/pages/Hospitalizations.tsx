import { useResource } from '../hooks/useResource'
import { useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { Editor, Page, Pagination, ResourceState, type Field } from '../components/ClinicUI'
import { request, type User } from '../services/api'
import { allPages, clinicApi, dateTime, labels, type Alimentacao, type Baia, type Internacao, type Jejum, type Paciente, type QrCode } from '../services/clinic'

export function Hospitalizations() {
  const { user } = useAuth()
  const [params] = useSearchParams()
  const [page, setPage] = useState(0)
  const [creating, setCreating] = useState(params.has('paciente'))
  const navigate = useNavigate()
  const resource = useResource(() => clinicApi.list<Internacao>('/internacoes/ativas',page), page)
  const options = useResource(async () => {
    const [patients,bays,vets] = await Promise.all([allPages<Paciente>('/pacientes'),allPages<Baia>('/baias'),user?.role === 'ADMIN' ? allPages<User>('/users?role=VETERINARIO') : Promise.resolve([])])
    return {patients,bays,vets:vets.filter(v => v.active && v.role === 'VETERINARIO')}
  }, user?.role ?? '')
  const fields: Field[] = [
    {name:'pacienteId',label:'Paciente',type:'number',value:params.get('paciente') ?? '',options:(options.data?.patients ?? []).map(p => ({value:p.id,label:`${p.nome} · ${p.especie} (#${p.id})`}))},
    {name:'baiaId',label:'Baia disponível',type:'number',options:(options.data?.bays ?? []).filter(b => b.status === 'DISPONIVEL').map(b => ({value:b.id,label:b.identificacao}))},
    {name:'veterinarioId',label:'Veterinário responsável',type:'number',min:'1',value:user?.role === 'VETERINARIO' ? user.id : '', ...(user?.role === 'ADMIN' ? {options:(options.data?.vets ?? []).map(v => ({value:v.id,label:v.name}))} : {hint:'Informe o código do veterinário cadastrado na equipe.'})},
    {name:'motivo',label:'Motivo da internação',type:'textarea'}, {name:'diagnosticoInicial',label:'Diagnóstico inicial',maxLength:255}, {name:'observacoes',label:'Observações',type:'textarea',required:false},
  ]
  return <Page title="Internações" description="Acompanhe os pacientes internados e organize o cuidado diário." action={<button className="clinic-primary" onClick={() => setCreating(true)}>+ Abrir internação</button>}>
    <ResourceState {...options} />
    {creating && options.data && <Editor title="Abrir internação" fields={fields} onCancel={() => setCreating(false)} onSave={async data => { const result = await clinicApi.save<Internacao>('/internacoes',data); navigate(`/internacoes/${result.id}`) }} />}
    <form className="clinic-toolbar" onSubmit={e => {e.preventDefault(); const id = new FormData(e.currentTarget).get('id'); navigate(`/internacoes/${id}`)}}><label>Consultar pelo código<input name="id" type="number" min="1" required placeholder="Código da internação" /></label><button>Consultar</button></form>
    <ResourceState {...resource} />
    {resource.data && <><div className="clinic-grid">{resource.data.items.map(i => <article className="clinic-card" key={i.id}><span className="clinic-badge">Internação #{i.id} · Ativa</span><h2>{options.data?.patients.find(p => p.id === i.pacienteId)?.nome ?? `Paciente #${i.pacienteId}`}</h2><p>Baia: {options.data?.bays.find(b => b.id === i.baiaId)?.identificacao ?? i.baiaId}</p><p>{i.motivo}</p><p>Entrada: {dateTime(i.entradaInternacao)}</p><Link to={`/internacoes/${i.id}`}>Acompanhar internação →</Link></article>)}</div>{!resource.data.total && <p className="clinic-state">Nenhuma internação ativa no momento.</p>}<Pagination {...resource.data} onChange={setPage} /></>}
  </Page>
}

export function HospitalizationDetail() {
  const { id = '' } = useParams()
  const { user } = useAuth()
  const [closing, setClosing] = useState(false)
  const [showQr, setShowQr] = useState(false)
  const resource = useResource(() => request<Internacao>(`/internacoes/${encodeURIComponent(id)}`), id)
  const identity = useResource(async () => {
    const detail = await request<Internacao>(`/internacoes/${encodeURIComponent(id)}`)
    const [patients,bay] = await Promise.all([allPages<Paciente>('/pacientes'),request<Baia>(`/baias/${detail.baiaId}`)])
    return {patient:patients.find(p => p.id === detail.pacienteId),bay}
  }, id)
  const i = resource.data
  const canCare = user?.role === 'ADMIN' || user?.role === 'VETERINARIO'
  return <Page title={identity.data?.patient?.nome ?? `Internação #${id}`} description="Dados da internação, identificação e histórico de cuidados." action={<Link className="clinic-button" to="/internacoes">Voltar às internações</Link>}>
    <ResourceState {...resource} /><ResourceState {...identity} />
    {i && <><section className="clinic-panel"><span className="clinic-badge">{labels[i.status]} · #{i.id}</span><dl className="clinic-details">{Object.entries({'Paciente':identity.data?.patient?.nome ?? `#${i.pacienteId}`,'Baia':identity.data?.bay.identificacao ?? `#${i.baiaId}`,'Veterinário':`#${i.veterinarioId}`,'Entrada':dateTime(i.entradaInternacao),'Saída':dateTime(i.saidaInternacao),'Motivo':i.motivo,'Diagnóstico inicial':i.diagnosticoInicial,'Observações':i.observacoes || '—'}).map(([label,value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl><div className="clinic-actions"><button onClick={() => setShowQr(!showQr)}>QR Code</button>{i.status === 'ATIVA' && <button onClick={() => setClosing(true)}>Encerrar internação</button>}</div></section>
    {showQr && <QrPanel id={id} />}
    {closing && <Editor title="Encerrar internação" submitLabel="Confirmar encerramento" fields={[{name:'statusEncerramento',label:'Desfecho (a baia será liberada)',options:[{value:'ALTA',label:'Alta'},{value:'OBITO',label:'Óbito'}]}]} onCancel={() => setClosing(false)} onSave={async data => { await clinicApi.save(`/internacoes/${id}/encerrar`,data,'PUT'); resource.reload() }} />}
    {canCare && <CarePanel id={id} active={i.status === 'ATIVA'} />}</>}
  </Page>
}
function QrPanel({ id }: { id: string }) {
  const resource = useResource(() => request<QrCode>(`/internacoes/${id}/qrcode`),id)
  return <section className="clinic-panel"><h2>Identificação por QR Code</h2><ResourceState {...resource} />{resource.data && <><img className="clinic-qr" src={`data:image/png;base64,${resource.data.base64}`} alt={`QR Code da internação ${id}`} /><div className="clinic-actions"><Link to={`/public/internacoes/qr/${resource.data.uuidToken}`}>Abrir consulta pública</Link><a className="clinic-button" href={`data:image/png;base64,${resource.data.base64}`} download={`internacao-${id}.png`}>Baixar QR Code</a></div></>}</section>
}
export function CarePanel({ id, active = true }: { id: string; active?: boolean }) {
  const [action, setAction] = useState<'food' | 'fast' | 'end' | null>(null)
  const resource = useResource(async () => {
    const [fasts,foods] = await Promise.all([request<Jejum[]>(`/internacoes/${id}/jejum`),request<Alimentacao[]>(`/internacoes/${id}/alimentacao`)])
    return {fasts,foods}
  }, id)
  const fasting = resource.data?.fasts.some(f => f.ativo)
  const fields: Field[] = action === 'fast' ? [{name:'motivo',label:'Motivo do jejum',maxLength:255}] : action === 'food' ? [{name:'alimento',label:'Alimento',maxLength:100},{name:'quantidade',label:'Quantidade (inclua a unidade)',maxLength:50},{name:'aceitacaoObservacao',label:'Aceitação e observações',type:'textarea',required:false}] : []
  return <section className="clinic-panel"><h2>Alimentação e jejum</h2><ResourceState {...resource} />{resource.data && <>{fasting && <p className="clinic-notice" role="status">Jejum ativo. Não oferecer alimento até o encerramento do jejum.</p>}{active && <div className="clinic-actions"><button disabled={fasting} onClick={() => setAction('food')}>Registrar alimentação</button><button onClick={() => setAction(fasting ? 'end' : 'fast')}>{fasting ? 'Encerrar jejum' : 'Iniciar jejum'}</button></div>}
    {action && <Editor title={action === 'food' ? 'Registrar alimentação' : action === 'fast' ? 'Iniciar jejum' : 'Confirmar encerramento do jejum'} fields={fields} onCancel={() => setAction(null)} onSave={async data => {await clinicApi.save(`/internacoes/${id}/${action === 'food' ? 'alimentacao' : action === 'fast' ? 'jejum' : 'jejum/encerrar'}`,action === 'end' ? undefined : data,action === 'end' ? 'PUT' : 'POST');resource.reload()}} />}
    <div className="clinic-grid"><div><h2>Histórico de alimentação</h2>{resource.data.foods.length === 0 && <p>Nenhuma alimentação registrada.</p>}{resource.data.foods.map(f => <article className="clinic-history" key={f.id}><strong>{f.alimento} · {f.quantidade}</strong><p>{f.aceitacaoObservacao || 'Sem observações.'}</p><small>{dateTime(f.dataHoraRegistro)} · Profissional #{f.usuarioId}</small></article>)}</div><div><h2>Histórico de jejum</h2>{resource.data.fasts.length === 0 && <p>Nenhum jejum registrado.</p>}{resource.data.fasts.map(f => <article className="clinic-history" key={f.id}><strong>{f.motivo}</strong><p>{f.ativo ? 'Em andamento' : 'Encerrado'}</p><small>Início: {dateTime(f.dataHoraInicio)}<br />Fim: {dateTime(f.dataHoraFim)}</small></article>)}</div></div></>}</section>
}
export function CareLookup() {
  const [id,setId] = useState('')
  return <Page title="Cuidados" description="Consulte o histórico e registre alimentação e jejum pelo código da internação."><form className="clinic-toolbar" onSubmit={e => {e.preventDefault();setId(String(new FormData(e.currentTarget).get('id')))}}><label>Código da internação<input name="id" type="number" min="1" required /></label><button className="clinic-primary">Consultar</button></form>{id && <CarePanel key={id} id={id} />}</Page>
}

