import { useState, type FormEvent } from 'react'
import { ArrowLeft, ArrowRight, Mail } from 'lucide-react'
import { Link } from 'react-router-dom'
import { RecoveryCard } from '../components/RecoveryCard'
import { apiRequest, errorMessage } from '../services/api'

export function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErro('')
    setEnviando(true)
    try {
      const response = await apiRequest<{ message: string }>('/auth/forgot-password', {
        method: 'POST', body: JSON.stringify({ email }),
      }, false)
      setMensagem(response.message)
    } catch (error) {
      setErro(errorMessage(error))
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
      {mensagem && <div className="api-feedback success" role="status">{mensagem}</div>}
      {erro && <div className="api-feedback error" role="alert">{erro}</div>}
      <form onSubmit={enviar}>
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
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </div>
        </div>

        <button className="btn auth-submit w-100 d-flex align-items-center justify-content-center gap-3" type="submit" disabled={enviando || Boolean(mensagem)}>
          {enviando ? <><span className="spinner-border spinner-border-sm" /> Enviando...</> : <>Enviar instruções <ArrowRight size={20} /></>}
        </button>
      </form>

      <Link className="recovery-back" to="/login"><ArrowLeft size={17} /> Voltar para o login</Link>
    </RecoveryCard>
  )
}
