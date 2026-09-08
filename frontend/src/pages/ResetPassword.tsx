import { useState, type FormEvent } from 'react'
import { ArrowLeft, ArrowRight, Check, Circle } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { RecoveryCard } from '../components/RecoveryCard'
import { authApi, readableError } from '../services/api'

export function ResetPassword({ invitation = false }: { invitation?: boolean }) {
  const [senha, setSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)
  const navigate = useNavigate()

  const requisitos = [
    { texto: 'Pelo menos 8 caracteres', atendido: senha.length >= 8 },
    { texto: 'Uma letra maiúscula', atendido: /[A-Z]/.test(senha) },
    { texto: 'Uma letra minúscula', atendido: /[a-z]/.test(senha) },
    { texto: 'Um número', atendido: /\d/.test(senha) },
    { texto: 'Um caractere especial', atendido: /[^A-Za-z0-9]/.test(senha) },
  ]

  const senhaValida = requisitos.every((requisito) => requisito.atendido)
  const senhasIguais = confirmacao.length > 0 && senha === confirmacao
  const formularioValido = senhaValida && senhasIguais

  async function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const token = new URLSearchParams(window.location.hash.slice(1)).get('token')
      ?? new URLSearchParams(window.location.search).get('token')
    if (!token) {
      setErro('Este link não contém um token válido. Solicite um novo e-mail.')
      return
    }

    setErro('')
    setEnviando(true)
    try {
      if (invitation) await authApi.confirmInvitation(token, senha)
      else await authApi.resetPassword(token, senha)
      navigate('/login', {
        replace: true,
        state: { message: invitation ? 'Cadastro confirmado. Agora você já pode entrar.' : 'Senha redefinida com sucesso.' },
      })
    } catch (error) {
      setErro(readableError(error))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <RecoveryCard
      eyebrow={invitation ? 'PRIMEIRO ACESSO' : 'NOVA SENHA'}
      title={invitation ? 'Confirme seu cadastro.' : 'Crie uma nova senha.'}
      description={invitation ? 'Escolha sua senha para ativar a conta e acessar o QRVet.' : 'Escolha uma senha segura e confirme-a antes de continuar.'}
    >
      <form onSubmit={enviar}>
        {erro && <p className="form-message error" role="alert">{erro}</p>}
        <div className="mb-4">
          <label className="form-label auth-label" htmlFor="nova-senha">Nova senha</label>
          <input
            className="form-control auth-input"
            id="nova-senha"
            name="novaSenha"
            type="password"
            placeholder="Digite sua nova senha"
            autoComplete="new-password"
            value={senha}
            onChange={(event) => setSenha(event.target.value)}
            required
          />

          <ul className="password-requirements" aria-label="Requisitos da senha" aria-live="polite">
            {requisitos.map((requisito) => (
              <li className={requisito.atendido ? 'requirement-valid' : ''} key={requisito.texto}>
                <span className="requirement-icon" aria-hidden="true">
                  {requisito.atendido ? <Check size={14} strokeWidth={3} /> : <Circle size={10} />}
                </span>
                {requisito.texto}
              </li>
            ))}
          </ul>
        </div>

        <div className="mb-4">
          <label className="form-label auth-label" htmlFor="confirmar-senha">Confirme a nova senha</label>
          <input
            className="form-control auth-input"
            id="confirmar-senha"
            name="confirmarSenha"
            type="password"
            placeholder="Digite a senha novamente"
            autoComplete="new-password"
            value={confirmacao}
            onChange={(event) => setConfirmacao(event.target.value)}
            aria-describedby="confirmacao-status"
            required
          />
          {confirmacao && (
            <p
              id="confirmacao-status"
              className={`password-match ${senhasIguais ? 'requirement-valid' : 'requirement-invalid'}`}
              role="status"
            >
              <span className="requirement-icon" aria-hidden="true">
                {senhasIguais ? <Check size={14} strokeWidth={3} /> : <Circle size={10} />}
              </span>
              {senhasIguais ? 'As senhas coincidem' : 'As senhas ainda não coincidem'}
            </p>
          )}
        </div>

        <button
          className="btn auth-submit w-100 d-flex align-items-center justify-content-center gap-3"
          type="submit"
          disabled={!formularioValido || enviando}
        >
          {enviando ? 'Salvando...' : invitation ? 'Ativar minha conta' : 'Redefinir minha senha'} {!enviando && <ArrowRight size={20} />}
        </button>
      </form>

      <Link className="recovery-back" to="/login"><ArrowLeft size={17} /> Voltar para o login</Link>
    </RecoveryCard>
  )
}
