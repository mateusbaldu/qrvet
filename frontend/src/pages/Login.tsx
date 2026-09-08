import { useRef, useState, type FormEvent } from 'react'
import { ArrowRight, Eye, EyeOff } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useLocation, useNavigate } from 'react-router-dom'
import { LoginCard } from '../components/LoginCard'
import { useAuth } from '../auth/useAuth'
import { readableError } from '../services/api'

export function Login() {
  const location = useLocation()
  const loginState = location.state as { from?: string; message?: string; email?: string } | null
  const [email, setEmail] = useState(loginState?.email ?? '')
  const [senha, setSenha] = useState('')
  const [mostrarSenha, setMostrarSenha] = useState(false)
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)
  const submissaoEmAndamento = useRef(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  async function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submissaoEmAndamento.current) return
    submissaoEmAndamento.current = true
    setErro('')
    setEnviando(true)
    try {
      const user = await login(email.trim(), senha)
      const requestedPath = loginState?.from
      navigate(requestedPath ?? (user.role === 'ADMIN' ? '/equipe' : '/meu-perfil'), { replace: true })
    } catch (error) {
      setErro(readableError(error))
      setSenha('')
    } finally {
      submissaoEmAndamento.current = false
      setEnviando(false)
    }
  }

  return (
    <LoginCard>
      <h2>Os pacientes estão a espera.  </h2>
      <p className="auth-description">Entre com sua conta para acessar</p>

      <form onSubmit={enviar}>
        {(erro || loginState?.message) && (
          <p className={erro ? 'form-message error' : 'form-message success'} role="alert">
            {erro || loginState?.message}
          </p>
        )}
        <div className="mb-4">
          <label className="form-label auth-label" htmlFor="email">Seu e-mail</label>
          <input className="form-control auth-input" id="email" name="email" type="email"
            placeholder="seuemail@exemplo.com" autoComplete="username" autoCapitalize="none"
            spellCheck={false} value={email} onChange={(event) => setEmail(event.target.value)} required />
        </div>

        <div className="mb-3">
          <label className="form-label auth-label" htmlFor="senha">Sua senha</label>
          <div className="auth-password-field">
            <input className="form-control auth-input" id="senha" name="senha"
              type={mostrarSenha ? 'text' : 'password'} autoComplete="current-password"
              value={senha} onChange={(event) => setSenha(event.target.value)} required />
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
        <button className="btn auth-submit w-100 d-flex align-items-center justify-content-center gap-3" type="submit" disabled={enviando}>
          {enviando ? 'Entrando...' : 'Entrar na minha conta'} {!enviando && <ArrowRight size={20} />}
        </button>
      </form>

      <hr className="auth-divider" />
      <p className="auth-register mb-2">Primeiro acesso ? </p>
      <p className="auth-invitation mb-0">Use o convite enviado ao seu e-mail pelo administrador.</p>
    </LoginCard>
  )
}
