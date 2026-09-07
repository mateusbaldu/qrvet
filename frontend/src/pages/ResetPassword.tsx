import { useState, type FormEvent } from 'react'
import { ArrowLeft, ArrowRight, Check, Circle } from 'lucide-react'
import { Link } from 'react-router-dom'
import { RecoveryCard } from '../components/RecoveryCard'

export function ResetPassword() {
  const [senha, setSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')

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

  function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
  }

  return (
    <RecoveryCard
      eyebrow="NOVA SENHA"
      title="Crie uma nova senha."
      description="Escolha uma senha segura e confirme-a antes de continuar."
    >
      <form onSubmit={enviar}>
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
          disabled={!formularioValido}
        >
          Redefinir minha senha <ArrowRight size={20} />
        </button>
      </form>

      <Link className="recovery-back" to="/login"><ArrowLeft size={17} /> Voltar para o login</Link>
    </RecoveryCard>
  )
}
