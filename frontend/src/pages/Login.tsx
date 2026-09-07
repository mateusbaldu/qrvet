import { useState, type FormEvent } from 'react'
import { ArrowRight, Eye, EyeOff } from 'lucide-react'
import { Link } from 'react-router-dom'
import { LoginCard } from '../components/LoginCard'

export function Login() {
  const [mostrarSenha, setMostrarSenha] = useState(false)

  function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
  }

  return (
    <LoginCard>
      <h2>Os pacientes estão a espera.  </h2>
      <p className="auth-description">Entre com sua conta para acessar</p>

      <form onSubmit={enviar}>
        <div className="mb-4">
          <label className="form-label auth-label" htmlFor="email">Seu e-mail</label>
          <input className="form-control auth-input" id="email" name="email" type="email"
            placeholder="seuemail@exemplo.com" autoComplete="email" required />
        </div>

        <div className="mb-3">
          <label className="form-label auth-label" htmlFor="senha">Sua senha</label>
          <div className="auth-password-field">
            <input className="form-control auth-input" id="senha" name="senha"
              type={mostrarSenha ? 'text' : 'password'} autoComplete="current-password" required />
            <button type="button" className="password-toggle"
              onClick={() => setMostrarSenha((atual) => !atual)}
              aria-label={mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'} aria-pressed={mostrarSenha}>
              {mostrarSenha ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>
        </div>

        <div className="text-end auth-forgot-row">
          <Link className="auth-link" to="/esqueci-senha">Esqueci minha senha</Link>
        </div>
        <button className="btn auth-submit w-100 d-flex align-items-center justify-content-center gap-3" type="submit">
          Entrar na minha conta <ArrowRight size={20} />
        </button>
      </form>

      <hr className="auth-divider" />
      <p className="auth-register mb-2">Primeiro acesso ? </p>
      <p className="auth-invitation mb-0">Use o convite enviado ao seu e-mail pelo administrador.</p>
    </LoginCard>
  )
}
