import type { FormEvent } from 'react'
import { ArrowLeft, ArrowRight, Mail } from 'lucide-react'
import { Link } from 'react-router-dom'
import { RecoveryCard } from '../components/RecoveryCard'

export function ForgotPassword() {
  function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
  }

  return (
    <RecoveryCard
      eyebrow="RECUPERAÇÃO DE ACESSO"
      title="Esqueceu sua senha?"
      description="Informe o e-mail da sua conta para receber as instruções de recuperação."
    >
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
              required
            />
          </div>
        </div>

        <button className="btn auth-submit w-100 d-flex align-items-center justify-content-center gap-3" type="submit">
          Enviar instruções <ArrowRight size={20} />
        </button>
      </form>

      <Link className="recovery-back" to="/login"><ArrowLeft size={17} /> Voltar para o login</Link>
    </RecoveryCard>
  )
}
