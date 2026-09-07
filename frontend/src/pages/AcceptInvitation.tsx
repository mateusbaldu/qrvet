import { useState, type FormEvent } from 'react'
import { ArrowLeft, ArrowRight, Check, Circle } from 'lucide-react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { RecoveryCard } from '../components/RecoveryCard'
import { apiRequest, errorMessage } from '../services/api'

export function AcceptInvitation() {
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [crmv, setCrmv] = useState('')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [error, setError] = useState('')
  const [sending, setSending] = useState(false)
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const token = params.get('token')

  const requirements = [
    { text: 'Pelo menos 8 caracteres', valid: password.length >= 8 },
    { text: 'Uma letra maiúscula', valid: /[A-Z]/.test(password) },
    { text: 'Uma letra minúscula', valid: /[a-z]/.test(password) },
    { text: 'Um número', valid: /\d/.test(password) },
    { text: 'Um caractere especial', valid: /[^A-Za-z0-9]/.test(password) },
  ]
  const valid = requirements.every((item) => item.valid) && password === confirmation && confirmation.length > 0

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token) return
    setSending(true)
    setError('')
    try {
      await apiRequest('/auth/accept-invitation', {
        method: 'POST',
        body: JSON.stringify({ token, name, phone: phone || null, crmv: crmv || null, password }),
      }, false)
      navigate('/login', { replace: true, state: { message: 'Conta criada. Entre com o e-mail que recebeu o convite.' } })
    } catch (requestError) {
      setError(errorMessage(requestError))
    } finally {
      setSending(false)
    }
  }

  return (
    <RecoveryCard eyebrow="PRIMEIRO ACESSO" title="Complete seu cadastro."
      description="Informe seus dados e crie uma senha para entrar na equipe da clínica.">
      {!token && <div className="api-feedback error" role="alert">Este link de convite não possui um token válido.</div>}
      {error && <div className="api-feedback error" role="alert">{error}</div>}
      <form onSubmit={submit}>
        <div className="mb-3">
          <label className="form-label auth-label" htmlFor="invite-name">Nome completo</label>
          <input className="form-control auth-input" id="invite-name" value={name} onChange={(event) => setName(event.target.value)} required />
        </div>
        <div className="row g-3 mb-3">
          <div className="col-12 col-md-6">
            <label className="form-label auth-label" htmlFor="invite-phone">Telefone</label>
            <input className="form-control auth-input" id="invite-phone" type="tel" value={phone} onChange={(event) => setPhone(event.target.value)} />
          </div>
          <div className="col-12 col-md-6">
            <label className="form-label auth-label" htmlFor="invite-crmv">CRMV, se aplicável</label>
            <input className="form-control auth-input" id="invite-crmv" value={crmv} onChange={(event) => setCrmv(event.target.value)} />
          </div>
        </div>
        <div className="mb-3">
          <label className="form-label auth-label" htmlFor="invite-password">Senha</label>
          <input className="form-control auth-input" id="invite-password" type="password" autoComplete="new-password"
            value={password} onChange={(event) => setPassword(event.target.value)} required />
          <ul className="password-requirements">
            {requirements.map((item) => <li className={item.valid ? 'requirement-valid' : ''} key={item.text}>
              <span className="requirement-icon">{item.valid ? <Check size={14} /> : <Circle size={10} />}</span>{item.text}
            </li>)}
          </ul>
        </div>
        <div className="mb-4">
          <label className="form-label auth-label" htmlFor="invite-confirmation">Confirmar senha</label>
          <input className="form-control auth-input" id="invite-confirmation" type="password" autoComplete="new-password"
            value={confirmation} onChange={(event) => setConfirmation(event.target.value)} required />
        </div>
        <button className="btn auth-submit w-100 d-flex align-items-center justify-content-center gap-3" disabled={!valid || !token || sending}>
          {sending ? <><span className="spinner-border spinner-border-sm" /> Criando conta...</> : <>Criar minha conta <ArrowRight size={20} /></>}
        </button>
      </form>
      <Link className="recovery-back" to="/login"><ArrowLeft size={17} /> Voltar para o login</Link>
    </RecoveryCard>
  )
}
