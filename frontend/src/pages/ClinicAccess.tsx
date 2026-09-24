import { useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ArrowRight, HeartPulse, PawPrint, QrCode as QrIcon, RefreshCw, ShieldCheck } from 'lucide-react'
import { useResource } from '../hooks/useResource'
import { Editor, EmptyState, Notice, Page, Pagination, ResourceState, StatusBadge } from '../components/ClinicUI'
import { request, type User } from '../services/api'
import { allPages, clinicApi, dateTime, type Session } from '../services/clinic'

export function PublicHospitalization() {
  const { token = '' } = useParams()
  const resource = useResource(() => clinicApi.publicDetails(token), token)
  const i = resource.data
  return <main className="clinic-public">
    <header className="public-brand"><span className="sidebar-logo"><PawPrint size={24} /></span><div><strong>QRVet</strong><small>Perto de quem você ama.</small></div><span className="public-readonly"><ShieldCheck size={15} />Consulta pública</span></header>
    <div className="clinic-page public-content"><p className="clinic-eyebrow">ACOMPANHAMENTO DA INTERNAÇÃO</p><ResourceState {...resource} />
      {i && <><section className="public-patient"><div className="clinic-section-heading"><span className="clinic-avatar large"><PawPrint size={30} /></span><StatusBadge status={i.status} /></div><h1>{i.pacienteNome}</h1><p>{i.especie} · {i.raca} · {i.sexo}</p><div className="public-location">Baia <strong>{i.baiaIdentificacao}</strong><span>Entrada em {dateTime(i.entradaInternacao)}</span></div></section>
        {i.jejumAtivo && i.status === 'ATIVA' && <p className="clinic-notice" role="status"><strong>Paciente em jejum.</strong><br />Não oferecer alimento. A equipe está acompanhando os cuidados.</p>}
        <section className="clinic-panel"><h2><HeartPulse size={20} />Informações da internação</h2><dl className="clinic-details single"><div><dt>Motivo da internação</dt><dd>{i.motivo}</dd></div><div><dt>Situação</dt><dd>{i.status === 'ATIVA' ? 'Paciente sob os cuidados da equipe veterinária.' : i.status === 'ALTA' ? 'Internação encerrada com alta.' : 'Internação encerrada por óbito.'}</dd></div></dl><button className="clinic-button" onClick={resource.reload}><RefreshCw size={16} />Atualizar informações</button></section>
        <p className="public-note">Para informações detalhadas sobre a evolução do paciente, entre em contato com a equipe da clínica.</p></>}
      <footer className="public-footer"><span><ShieldCheck size={15} />Cuidado com responsabilidade</span><Link to="/login">Acesso da equipe <ArrowRight size={14} /></Link></footer>
    </div>
  </main>
}

export function QrLookup() {
  const [error, setError] = useState('')
  const navigate = useNavigate()
  return <Page title="Consultar QR Code" description="Um acesso rápido ao acompanhamento de quem está sob nossos cuidados.">
    <div className="clinic-lookup-grid"><form className="clinic-panel clinic-lookup-card" onSubmit={e => {
      e.preventDefault()
      const raw = String(new FormData(e.currentTarget).get('token')).trim()
      let token = raw
      try { token = new URL(raw).pathname.replace(/\/$/, '').split('/').pop() ?? '' } catch { /* Também aceita o UUID sem endereço. */ }
      if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(token)) { setError('Informe um código ou endereço de QR Code válido.'); return }
      navigate('/public/internacoes/qr/' + token)
    }}><span className="clinic-lookup-icon"><QrIcon size={48} strokeWidth={1.5} /></span><h2>Encontre a internação</h2><p>Cole o link compartilhado pela clínica ou digite o código completo do QR Code.</p><label>Endereço ou código do QR Code<input name="token" required placeholder="Cole o link ou o código aqui" autoComplete="off" onChange={() => setError('')} /></label>{error && <p role="alert" className="clinic-error">{error}</p>}<button className="clinic-primary">Consultar internação<ArrowRight size={17} /></button></form>
    <aside className="clinic-panel clinic-lookup-help"><h2>Como acessar pelo celular</h2><ol><li>Abra a câmera do seu celular.</li><li>Aponte para o QR Code fornecido pela clínica.</li><li>Toque no link para acompanhar a internação.</li></ol><p><ShieldCheck size={18} />O acompanhamento pelo link não exige login.</p></aside></div>
  </Page>
}

export function Sessions() {
  const [params] = useSearchParams()
  const [userId, setUserId] = useState(params.get('usuario') ?? '')
  const [page, setPage] = useState(0)
  const [revoke, setRevoke] = useState<Session | 'all' | null>(null)
  const [success, setSuccess] = useState('')
  const users = useResource(() => allPages<User>('/users'), 'users')
  const resource = useResource(() => userId ? clinicApi.list<Session>('/users/' + userId + '/sessions', page) : Promise.resolve({ items: [], page: 0, size: 12, total: 0, pages: 0 }), userId + ':' + page)
  return <Page title="Sessões da equipe" description="Acompanhe os acessos e encerre sessões quando necessário." action={<Link to="/equipe" className="clinic-button">Voltar à equipe</Link>}>
    {success && <Notice>{success}</Notice>}<ResourceState {...users} /><div className="clinic-panel"><label>Membro da equipe<select value={userId} onChange={e => { setUserId(e.target.value); setPage(0); setRevoke(null); setSuccess('') }}><option value="">Selecione um membro</option>{users.data?.map(u => <option value={u.id} key={u.id}>{u.name} · {u.email}</option>)}</select></label></div>
    {userId ? <><ResourceState {...resource} />{revoke && <Editor title={revoke === 'all' ? 'Encerrar todas as sessões deste usuário?' : 'Encerrar esta sessão?'} description="O membro precisará entrar novamente. Confira o usuário selecionado antes de confirmar." fields={[]} submitLabel="Confirmar encerramento" onCancel={() => setRevoke(null)} onSave={async () => { await request('/users/' + userId + '/sessions' + (revoke === 'all' ? '' : '/' + encodeURIComponent(revoke.jti)), { method: 'DELETE' }); setSuccess('Acesso encerrado com sucesso.'); setPage(0); resource.reload() }} />}
      {resource.data && <><div className="clinic-actions"><button disabled={!resource.data.total} onClick={() => setRevoke('all')}>Encerrar todas as sessões</button></div><div className="clinic-grid">{resource.data.items.map(s => <article className="clinic-card" key={s.jti}><span className="clinic-avatar"><ShieldCheck size={23} /></span><h2>Sessão ativa</h2><dl className="clinic-details single"><div><dt>Início</dt><dd>{dateTime(s.createdAt)}</dd></div><div><dt>Expiração</dt><dd>{dateTime(s.expiresAt)}</dd></div></dl><button onClick={() => setRevoke(s)}>Encerrar sessão</button></article>)}</div>{!resource.data.total && <EmptyState title="Nenhuma sessão ativa"><p>Este membro não possui acessos ativos no momento.</p></EmptyState>}<Pagination {...resource.data} onChange={setPage} /></>}</> : <EmptyState title="Selecione um membro"><p>Os acessos ativos serão exibidos aqui.</p></EmptyState>}
  </Page>
}

export function Bootstrap() {
  const navigate = useNavigate()
  const [done, setDone] = useState(false)
  return <main className="clinic-public"><Page title="Configurar clínica" description="Crie a primeira conta de administrador para começar.">{done ? <section className="clinic-panel"><Notice>Administrador criado com sucesso.</Notice><Link to="/login">Entrar</Link></section> : <Editor inline closeOnSave={false} title="Primeiro administrador" fields={[{ name: 'name', label: 'Nome completo', maxLength: 120 }, { name: 'email', label: 'E-mail', type: 'email', maxLength: 254 }, { name: 'password', label: 'Senha (mínimo de 8 caracteres)', type: 'password', minLength: 8, maxLength: 72, autoComplete: 'new-password' }, { name: 'secret', label: 'Chave de configuração', type: 'password' }]} onCancel={() => { if (!done) navigate('/login') }} onSave={async data => { await request('/bootstrap/admin', { method: 'POST', headers: { 'X-Bootstrap-Secret': String(data.secret) }, body: { name: data.name, email: data.email, password: data.password }, retryAuthentication: false, authenticated: false }); setDone(true) }} />}<Link to="/login">Voltar ao acesso</Link></Page></main>
}


