import { useState, type FormEvent } from 'react'
import { ArrowLeft, ArrowRight, Mail } from 'lucide-react'
import { Link } from 'react-router-dom'
import { RecoveryCard } from '../components/RecoveryCard'
import { authApi, readableError } from '../services/api'

export function ForgotPassword() {
  const [enviando, setEnviando] = useState(false)
  const [mensagem, setMensagem] = useState('')
  const [erro, setErro] = useState('')

  async function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const data = new FormData(form)
    setErro('')
    setMensagem('')
    setEnviando(true)
    try {
      await authApi.forgotPassword(String(data.get('email')))
      setMensagem('Se a conta estiver apta, as instruções serão enviadas para o e-mail informado.')
      form.reset()
    } catch (error) {
      setErro(readableError(error))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <RecoveryCard
      eyebrow="RECUPERAÇÃO DE ACESSO"
      title="Esqueceu sua senha?"
      description="Informe o e-mail da sua conta para receber as instruções de recuperação."
    >
      <form onSubmit={enviar}>
        {(erro || mensagem) && <p className={`form-message ${erro ? 'error' : 'success'}`} role="alert">{erro || mensagem}</p>}
        <div className="mb-4">
          <label className="form-label auth-label" htmlFor="email-recuperacao">Seu e-mail</label>
          <div className="recovery-input-icon">
            <Mail size={19} aria-hidden="true" />
            <input
              className="form-control auth-input"
              id="email-recuperacao"
              name="email"
              type="email"
              placeholder="seuemail@exemplo.com"
              autoComplete="email"
              required
            />
          </div>
        </div>

        <button className="btn auth-submit w-100 d-flex align-items-center justify-content-center gap-3" type="submit" disabled={enviando}>
          {enviando ? 'Enviando...' : 'Enviar instruções'} {!enviando && <ArrowRight size={20} />}
        </button>
      </form>

      <Link className="recovery-back" to="/login"><ArrowLeft size={17} /> Voltar para o login</Link>
    </RecoveryCard>
  )
}
