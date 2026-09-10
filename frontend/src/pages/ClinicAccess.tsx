import { useResource } from '../hooks/useResource'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Editor, Page, Pagination, ResourceState } from '../components/ClinicUI'
import { request, type User } from '../services/api'
import { allPages, clinicApi, dateTime, labels, type Session } from '../services/clinic'

export function PublicHospitalization() {
  const { token = '' } = useParams()
  const resource = useResource(() => clinicApi.publicDetails(token),token)
  const i = resource.data
  return <main className="clinic-public"><Page title="QRVet" description="Acompanhamento da internação"><ResourceState {...resource} />{i && <article className="clinic-panel"><span className="clinic-badge">{labels[i.status] ?? i.status}</span><h1>{i.pacienteNome}</h1><p>{i.especie} · {i.raca} · {i.sexo}</p>{i.jejumAtivo && <p className="clinic-notice">Paciente em jejum. Não oferecer alimento.</p>}<dl className="clinic-details"><div><dt>Baia</dt><dd>{i.baiaIdentificacao}</dd></div><div><dt>Entrada</dt><dd>{dateTime(i.entradaInternacao)}</dd></div><div><dt>Motivo da internação</dt><dd>{i.motivo}</dd></div></dl><button onClick={resource.reload}>Atualizar informações</button></article>}<Link to="/login">Acesso da equipe</Link></Page></main>
}
export function QrLookup() {
  const [error,setError] = useState('')
  const navigate = useNavigate()
  return <Page title="Consultar QR Code" description="Cole o endereço ou o código de identificação presente no QR Code."><form className="clinic-panel" onSubmit={e => {e.preventDefault();const raw = String(new FormData(e.currentTarget).get('token')).trim();const token = raw.replace(/\/$/,'').split('/').pop() ?? '';if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(token)) {setError('Informe um código ou endereço de QR Code válido.');return}navigate(`/public/internacoes/qr/${token}`)}}><label>Endereço ou código do QR Code<input name="token" required /></label>{error && <p role="alert" className="clinic-error">{error}</p>}<div className="clinic-actions"><button className="clinic-primary">Consultar</button></div></form></Page>
}
export function Sessions() {
  const [userId,setUserId] = useState('')
  const [page,setPage] = useState(0)
  const [revoke,setRevoke] = useState<Session | 'all' | null>(null)
  const users = useResource(() => allPages<User>('/users'),'users')
  const resource = useResource(() => userId ? clinicApi.list<Session>(`/users/${userId}/sessions`,page) : Promise.resolve({items:[],page:0,size:12,total:0,pages:0}),`${userId}:${page}`)
  return <Page title="Sessões da equipe" description="Consulte acessos ativos e encerre sessões de um membro da equipe."><ResourceState {...users} /><label>Membro da equipe<select value={userId} onChange={e => {setUserId(e.target.value);setPage(0);setRevoke(null)}}><option value="">Selecione um membro</option>{users.data?.map(u => <option value={u.id} key={u.id}>{u.name} · {u.email}</option>)}</select></label>{userId && <><ResourceState {...resource} />{revoke && <Editor title={revoke === 'all' ? 'Encerrar todas as sessões deste usuário?' : 'Encerrar esta sessão?'} fields={[]} submitLabel="Confirmar encerramento" onCancel={() => setRevoke(null)} onSave={async () => {await request(`/users/${userId}/sessions${revoke === 'all' ? '' : `/${encodeURIComponent(revoke.jti)}`}`,{method:'DELETE'});resource.reload()}} />}{resource.data && <><div className="clinic-actions"><button disabled={!resource.data.total} onClick={() => setRevoke('all')}>Encerrar todas as sessões</button></div>{resource.data.items.map(s => <article className="clinic-panel" key={s.jti}><p>Início: {dateTime(s.createdAt)}<br />Expira em: {dateTime(s.expiresAt)}</p><button onClick={() => setRevoke(s)}>Encerrar sessão</button></article>)}{!resource.data.total && <p className="clinic-state">Nenhuma sessão ativa.</p>}<Pagination {...resource.data} onChange={setPage} /></>}</>}</Page>
}
export function Bootstrap() {
  const navigate = useNavigate()
  const [done,setDone] = useState(false)
  return <main className="clinic-public"><Page title="Configurar clínica" description="Criação do primeiro administrador. Disponível somente durante a configuração inicial.">{done ? <section className="clinic-panel"><p role="status">Administrador criado com sucesso.</p><Link to="/login">Entrar</Link></section> : <Editor title="Primeiro administrador" fields={[{name:'name',label:'Nome completo',maxLength:120},{name:'email',label:'E-mail',type:'email',maxLength:254},{name:'password',label:'Senha (mínimo de 8 caracteres)',type:'password',minLength:8,maxLength:72},{name:'secret',label:'Chave de configuração',type:'password'}]} onCancel={() => navigate('/login')} onSave={async data => {if(String(data.password).length < 8) throw new Error('Senha muito curta');await request('/bootstrap/admin',{method:'POST',headers:{'X-Bootstrap-Secret':String(data.secret)},body:{name:data.name,email:data.email,password:data.password},retryAuthentication:false});setDone(true)}} />}<Link to="/login">Voltar ao acesso</Link></Page></main>
}


