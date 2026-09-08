import { useState, type FormEvent } from 'react'
import { Check, Circle, KeyRound, ShieldCheck, UserRound } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { authApi, readableError, type Role } from '../services/api'
import '../styles/profile.css'

const roleLabels: Record<Role, string> = {
  ADMIN: 'Administrador(a)', VETERINARIO: 'Veterinário(a)', RECEPCIONISTA: 'Recepcionista',
  AUXILIAR_TECNICO: 'Auxiliar técnico(a)', TUTOR: 'Tutor(a)',
}

function initials(name: string) {
  return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase()).join('') || '—'
}

export function Profile() {
  const { user, clearSession } = useAuth()
  const navigate = useNavigate()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (!user) return null

  const requirements = [
    { text: 'Pelo menos 8 caracteres', met: newPassword.length >= 8 },
    { text: 'Uma letra maiúscula', met: /[A-Z]/.test(newPassword) },
    { text: 'Uma letra minúscula', met: /[a-z]/.test(newPassword) },
    { text: 'Um número', met: /\d/.test(newPassword) },
    { text: 'Um caractere especial', met: /[^A-Za-z0-9]/.test(newPassword) },
  ]
  const matching = confirmation.length > 0 && newPassword === confirmation
  const canChange = currentPassword.length > 0 && requirements.every((item) => item.met) && matching

  async function changePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(''); setSubmitting(true)
    try {
      await authApi.changePassword(currentPassword, newPassword)
      clearSession()
      navigate('/login', { replace: true, state: { message: 'Senha alterada. Entre novamente com sua nova senha.' } })
    } catch (requestError) {
      setError(readableError(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  return <main className="profile-page"><div className="profile-container">
    <header className="profile-heading"><div><span className="profile-eyebrow"><UserRound size={15} /> CONFIGURAÇÕES DA CONTA</span><h1>Meu perfil</h1><p>Consulte seus dados e altere sua credencial de acesso.</p></div></header>
    {error && <p className="form-message error" role="alert">{error}</p>}
    <div className="row g-4 align-items-start">
      <aside className="col-12 col-lg-4"><section className="profile-card profile-identity-card">
        <div className="profile-photo"><span>{initials(user.name)}</span></div>
        <h2>{user.name}</h2><p>{user.email}</p><span className="profile-role"><ShieldCheck size={14} /> {roleLabels[user.role]}</span>
        <small className="mt-4">O backend atual ainda não disponibiliza upload de foto.</small>
      </section></aside>

      <div className="col-12 col-lg-8 d-grid gap-4">
        <section className="profile-card"><div className="profile-section-heading"><span className="profile-section-icon"><UserRound size={19} /></span><div><h2>Dados pessoais</h2><p>Informações cadastradas para sua conta.</p></div></div>
          <div className="row g-3"><div className="col-12 col-md-6"><label className="form-label auth-label" htmlFor="nome-perfil">Nome completo</label><input className="form-control profile-input" id="nome-perfil" value={user.name} readOnly /></div><div className="col-12 col-md-6"><label className="form-label auth-label" htmlFor="email-perfil">E-mail</label><input className="form-control profile-input" id="email-perfil" type="email" value={user.email} readOnly /></div></div>
          <p className="profile-api-note">A edição desses campos ainda precisa de um endpoint no backend.</p>
        </section>

        <section className="profile-card"><div className="profile-section-heading"><span className="profile-section-icon"><KeyRound size={19} /></span><div><h2>Alterar senha</h2><p>Ao alterar, sua sessão será encerrada por segurança.</p></div></div>
          <form onSubmit={changePassword}>
            <div className="mb-3"><label className="form-label auth-label" htmlFor="senha-atual-perfil">Senha atual</label><input className="form-control profile-input" id="senha-atual-perfil" type="password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} autoComplete="current-password" required /></div>
            <div className="row g-3"><div className="col-12 col-md-6"><label className="form-label auth-label" htmlFor="nova-senha-perfil">Nova senha</label><input className="form-control profile-input" id="nova-senha-perfil" type="password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} autoComplete="new-password" required /></div><div className="col-12 col-md-6"><label className="form-label auth-label" htmlFor="confirmar-senha-perfil">Confirmar nova senha</label><input className="form-control profile-input" id="confirmar-senha-perfil" type="password" value={confirmation} onChange={(event) => setConfirmation(event.target.value)} autoComplete="new-password" required /></div></div>
            <ul className="profile-password-requirements" aria-label="Requisitos da nova senha" aria-live="polite">{requirements.map((item) => <li className={item.met ? 'requirement-valid' : ''} key={item.text}><span className="requirement-icon">{item.met ? <Check size={14} strokeWidth={3} /> : <Circle size={10} />}</span>{item.text}</li>)}<li className={matching ? 'requirement-valid' : ''}><span className="requirement-icon">{matching ? <Check size={14} strokeWidth={3} /> : <Circle size={10} />}</span>As senhas coincidem</li></ul>
            <div className="profile-form-actions"><button className="btn profile-primary-button" type="submit" disabled={!canChange || submitting}><KeyRound size={17} /> {submitting ? 'Alterando...' : 'Alterar senha'}</button></div>
          </form>
        </section>
      </div>
    </div>
  </div></main>
}
