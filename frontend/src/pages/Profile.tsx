import { useState, type ChangeEvent, type FormEvent } from 'react'
import { Camera, Check, Circle, KeyRound, Save, ShieldCheck, Trash2, UserRound } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/auth-context'
import { apiRequest, errorMessage, initials, roleLabel, type User } from '../services/api'
import '../styles/profile.css'

export function Profile() {
  const { user, updateUser, logout } = useAuth()
  const navigate = useNavigate()
  if (!user) return null

  return <ProfileContent key={`${user.id}-${user.email}`} user={user} updateUser={updateUser} logout={logout} navigate={navigate} />
}

function ProfileContent({ user, updateUser, logout, navigate }: {
  user: User; updateUser: (user: User) => void; logout: () => Promise<void>; navigate: ReturnType<typeof useNavigate>
}) {
  const [nome, setNome] = useState(user.name)
  const [email, setEmail] = useState(user.email)
  const [telefone, setTelefone] = useState(user.phone || '')
  const [crmv, setCrmv] = useState(user.crmv || '')
  const [senhaAtual, setSenhaAtual] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [fotoVersion, setFotoVersion] = useState(0)
  const [salvando, setSalvando] = useState(false)
  const [enviandoFoto, setEnviandoFoto] = useState(false)
  const [alterandoSenha, setAlterandoSenha] = useState(false)
  const [erro, setErro] = useState('')
  const [mensagem, setMensagem] = useState('')

  const requisitos = [
    { texto: 'Pelo menos 8 caracteres', atendido: novaSenha.length >= 8 },
    { texto: 'Uma letra maiúscula', atendido: /[A-Z]/.test(novaSenha) },
    { texto: 'Uma letra minúscula', atendido: /[a-z]/.test(novaSenha) },
    { texto: 'Um número', atendido: /\d/.test(novaSenha) },
    { texto: 'Um caractere especial', atendido: /[^A-Za-z0-9]/.test(novaSenha) },
  ]
  const senhaValida = requisitos.every((item) => item.atendido)
  const senhasIguais = confirmacao.length > 0 && novaSenha === confirmacao
  const podeAlterarSenha = senhaAtual.length > 0 && senhaValida && senhasIguais

  async function escolherFoto(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) return
    setErro(''); setMensagem(''); setEnviandoFoto(true)
    try {
      const data = new FormData(); data.append('file', file)
      const updated = await apiRequest<User>('/users/me/avatar', { method: 'POST', body: data })
      updateUser(updated); setFotoVersion(Date.now()); setMensagem('Foto atualizada com sucesso.')
    } catch (error) { setErro(errorMessage(error)) } finally { setEnviandoFoto(false); event.target.value = '' }
  }

  async function removerFoto() {
    setErro(''); setMensagem(''); setEnviandoFoto(true)
    try {
      await apiRequest('/users/me/avatar', { method: 'DELETE' })
      updateUser({ ...user, avatarUrl: null }); setMensagem('Foto removida.')
    } catch (error) { setErro(errorMessage(error)) } finally { setEnviandoFoto(false) }
  }

  async function salvarDados(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setErro(''); setMensagem(''); setSalvando(true)
    try {
      const updated = await apiRequest<User>('/users/me', {
        method: 'PATCH', body: JSON.stringify({ name: nome, email, phone: telefone || null, crmv: crmv || null }),
      })
      updateUser(updated); setMensagem('Dados pessoais atualizados.')
    } catch (error) { setErro(errorMessage(error)) } finally { setSalvando(false) }
  }

  async function alterarSenha(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setErro(''); setMensagem(''); setAlterandoSenha(true)
    try {
      await apiRequest('/users/me/password', { method: 'PATCH', body: JSON.stringify({ currentPassword: senhaAtual, newPassword: novaSenha }) })
      await logout().catch(() => undefined)
      navigate('/login', { replace: true, state: { message: 'Senha alterada. Entre novamente com a nova senha.' } })
    } catch (error) { setErro(errorMessage(error)); setAlterandoSenha(false) }
  }

  const avatarSrc = user.avatarUrl ? `${user.avatarUrl}?v=${fotoVersion}` : null

  return <main className="profile-page"><div className="profile-container">
    <header className="profile-heading"><div><span className="profile-eyebrow"><UserRound size={15} /> CONFIGURAÇÕES DA CONTA</span><h1>Meu perfil</h1><p>Atualize seus dados pessoais, foto e credenciais de acesso.</p></div></header>
    {mensagem && <div className="api-feedback success" role="status">{mensagem}</div>}
    {erro && <div className="api-feedback error" role="alert">{erro}</div>}
    <div className="row g-4 align-items-start">
      <aside className="col-12 col-lg-4"><section className="profile-card profile-identity-card">
        <div className="profile-photo">{avatarSrc ? <img src={avatarSrc} alt="Foto de perfil" /> : <span>{initials(user.name)}</span>}
          <label className="profile-photo-button" htmlFor="foto-perfil" aria-label="Escolher foto"><Camera size={17} /></label>
          <input id="foto-perfil" className="visually-hidden" type="file" accept="image/png,image/jpeg,image/webp" onChange={escolherFoto} disabled={enviandoFoto} />
        </div>
        <h2>{user.name}</h2><p>{user.email}</p><span className="profile-role"><ShieldCheck size={14} /> {roleLabel(user.role)}</span>
        <div className="profile-photo-actions"><label className="btn profile-secondary-button" htmlFor="foto-perfil"><Camera size={16} /> {enviandoFoto ? 'Enviando...' : 'Alterar foto'}</label>
          {user.avatarUrl && <button className="btn profile-remove-photo" type="button" onClick={removerFoto} disabled={enviandoFoto}><Trash2 size={16} /> Remover</button>}
        </div><small>PNG, JPG ou WebP. Tamanho máximo: 5 MB.</small>
      </section></aside>

      <div className="col-12 col-lg-8 d-grid gap-4">
        <section className="profile-card"><div className="profile-section-heading"><span className="profile-section-icon"><UserRound size={19} /></span><div><h2>Dados pessoais</h2><p>Informações exibidas para a equipe da clínica.</p></div></div>
          <form onSubmit={salvarDados}><div className="row g-3">
            <div className="col-12 col-md-6"><label className="form-label auth-label" htmlFor="nome-perfil">Nome completo</label><input className="form-control profile-input" id="nome-perfil" value={nome} onChange={(event) => setNome(event.target.value)} required /></div>
            <div className="col-12 col-md-6"><label className="form-label auth-label" htmlFor="email-perfil">E-mail</label><input className="form-control profile-input" id="email-perfil" type="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></div>
            <div className="col-12 col-md-6"><label className="form-label auth-label" htmlFor="telefone-perfil">Telefone</label><input className="form-control profile-input" id="telefone-perfil" type="tel" value={telefone} onChange={(event) => setTelefone(event.target.value)} /></div>
            <div className="col-12 col-md-6"><label className="form-label auth-label" htmlFor="crmv-perfil">CRMV</label><input className="form-control profile-input" id="crmv-perfil" value={crmv} onChange={(event) => setCrmv(event.target.value)} /></div>
          </div><div className="profile-form-actions"><button className="btn profile-primary-button" disabled={salvando}><Save size={17} /> {salvando ? 'Salvando...' : 'Salvar alterações'}</button></div></form>
        </section>

        <section className="profile-card"><div className="profile-section-heading"><span className="profile-section-icon"><KeyRound size={19} /></span><div><h2>Alterar senha</h2><p>Use uma senha forte que você não utiliza em outros serviços.</p></div></div>
          <form onSubmit={alterarSenha}><div className="mb-3"><label className="form-label auth-label" htmlFor="senha-atual-perfil">Senha atual</label><input className="form-control profile-input" id="senha-atual-perfil" type="password" value={senhaAtual} onChange={(event) => setSenhaAtual(event.target.value)} autoComplete="current-password" required /></div>
            <div className="row g-3"><div className="col-12 col-md-6"><label className="form-label auth-label" htmlFor="nova-senha-perfil">Nova senha</label><input className="form-control profile-input" id="nova-senha-perfil" type="password" value={novaSenha} onChange={(event) => setNovaSenha(event.target.value)} autoComplete="new-password" required /></div>
              <div className="col-12 col-md-6"><label className="form-label auth-label" htmlFor="confirmar-senha-perfil">Confirmar nova senha</label><input className="form-control profile-input" id="confirmar-senha-perfil" type="password" value={confirmacao} onChange={(event) => setConfirmacao(event.target.value)} autoComplete="new-password" required /></div></div>
            <ul className="profile-password-requirements">{requisitos.map((item) => <li className={item.atendido ? 'requirement-valid' : ''} key={item.texto}><span className="requirement-icon">{item.atendido ? <Check size={14} /> : <Circle size={10} />}</span>{item.texto}</li>)}<li className={senhasIguais ? 'requirement-valid' : ''}><span className="requirement-icon">{senhasIguais ? <Check size={14} /> : <Circle size={10} />}</span>As senhas coincidem</li></ul>
            <div className="profile-form-actions"><button className="btn profile-primary-button" disabled={!podeAlterarSenha || alterandoSenha}><KeyRound size={17} /> {alterandoSenha ? 'Alterando...' : 'Alterar senha'}</button></div>
          </form>
        </section>
      </div>
    </div>
  </div></main>
}
