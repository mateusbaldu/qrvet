import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { Camera, Check, Circle, KeyRound, Save, ShieldCheck, Trash2, UserRound } from 'lucide-react'
import '../styles/profile.css'

export function Profile() {
  const [foto, setFoto] = useState<string | null>(null)
  const [nome, setNome] = useState('Dra. Sarah Jenkins')
  const [email, setEmail] = useState('sarah@qrvet.clinic')
  const [senhaAtual, setSenhaAtual] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')

  useEffect(() => {
    return () => {
      if (foto) URL.revokeObjectURL(foto)
    }
  }, [foto])

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

  function escolherFoto(event: ChangeEvent<HTMLInputElement>) {
    const arquivo = event.target.files?.[0]
    if (arquivo) setFoto(URL.createObjectURL(arquivo))
  }

  function salvarDados(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
  }

  function alterarSenha(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
  }

  return (
    <main className="profile-page">
      <div className="profile-container">
        <header className="profile-heading">
          <div>
            <span className="profile-eyebrow"><UserRound size={15} /> CONFIGURAÇÕES DA CONTA</span>
            <h1>Meu perfil</h1>
            <p>Atualize seus dados pessoais, foto e credenciais de acesso.</p>
          </div>
        </header>

        <div className="row g-4 align-items-start">
          <aside className="col-12 col-lg-4">
            <section className="profile-card profile-identity-card">
              <div className="profile-photo">
                {foto ? <img src={foto} alt="Pré-visualização da foto de perfil" /> : <span>DS</span>}
                <label className="profile-photo-button" htmlFor="foto-perfil" aria-label="Escolher foto">
                  <Camera size={17} />
                </label>
                <input id="foto-perfil" className="visually-hidden" type="file" accept="image/png,image/jpeg,image/webp" onChange={escolherFoto} />
              </div>

              <h2>{nome || 'Seu nome'}</h2>
              <p>{email || 'seuemail@exemplo.com'}</p>
              <span className="profile-role"><ShieldCheck size={14} /> Administradora</span>

              <div className="profile-photo-actions">
                <label className="btn profile-secondary-button" htmlFor="foto-perfil"><Camera size={16} /> Alterar foto</label>
                {foto && (
                  <button className="btn profile-remove-photo" type="button" onClick={() => setFoto(null)}>
                    <Trash2 size={16} /> Remover
                  </button>
                )}
              </div>
              <small>PNG, JPG ou WebP. Tamanho máximo recomendado: 5 MB.</small>
            </section>
          </aside>

          <div className="col-12 col-lg-8 d-grid gap-4">
            <section className="profile-card">
              <div className="profile-section-heading">
                <span className="profile-section-icon"><UserRound size={19} /></span>
                <div><h2>Dados pessoais</h2><p>Informações exibidas para a equipe da clínica.</p></div>
              </div>

              <form onSubmit={salvarDados}>
                <div className="row g-3">
                  <div className="col-12 col-md-6">
                    <label className="form-label auth-label" htmlFor="nome-perfil">Nome completo</label>
                    <input className="form-control profile-input" id="nome-perfil" value={nome} onChange={(event) => setNome(event.target.value)} required />
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label auth-label" htmlFor="email-perfil">E-mail</label>
                    <input className="form-control profile-input" id="email-perfil" type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label auth-label" htmlFor="telefone-perfil">Telefone</label>
                    <input className="form-control profile-input" id="telefone-perfil" type="tel" defaultValue="(11) 99999-1234" />
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label auth-label" htmlFor="crmv-perfil">CRMV</label>
                    <input className="form-control profile-input" id="crmv-perfil" defaultValue="CRMV-SP 12345" />
                  </div>
                </div>
                <div className="profile-form-actions">
                  <button className="btn profile-primary-button" type="submit"><Save size={17} /> Salvar alterações</button>
                </div>
              </form>
            </section>

            <section className="profile-card">
              <div className="profile-section-heading">
                <span className="profile-section-icon"><KeyRound size={19} /></span>
                <div><h2>Alterar senha</h2><p>Use uma senha forte que você não utiliza em outros serviços.</p></div>
              </div>

              <form onSubmit={alterarSenha}>
                <div className="mb-3">
                  <label className="form-label auth-label" htmlFor="senha-atual-perfil">Senha atual</label>
                  <input className="form-control profile-input" id="senha-atual-perfil" type="password" value={senhaAtual}
                    onChange={(event) => setSenhaAtual(event.target.value)} autoComplete="current-password" required />
                </div>
                <div className="row g-3">
                  <div className="col-12 col-md-6">
                    <label className="form-label auth-label" htmlFor="nova-senha-perfil">Nova senha</label>
                    <input className="form-control profile-input" id="nova-senha-perfil" type="password" value={novaSenha}
                      onChange={(event) => setNovaSenha(event.target.value)} autoComplete="new-password" required />
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label auth-label" htmlFor="confirmar-senha-perfil">Confirmar nova senha</label>
                    <input className="form-control profile-input" id="confirmar-senha-perfil" type="password" value={confirmacao}
                      onChange={(event) => setConfirmacao(event.target.value)} autoComplete="new-password" required />
                  </div>
                </div>

                <ul className="profile-password-requirements" aria-label="Requisitos da nova senha" aria-live="polite">
                  {requisitos.map((item) => (
                    <li className={item.atendido ? 'requirement-valid' : ''} key={item.texto}>
                      <span className="requirement-icon" aria-hidden="true">
                        {item.atendido ? <Check size={14} strokeWidth={3} /> : <Circle size={10} />}
                      </span>
                      {item.texto}
                    </li>
                  ))}
                  <li className={senhasIguais ? 'requirement-valid' : ''}>
                    <span className="requirement-icon" aria-hidden="true">
                      {senhasIguais ? <Check size={14} strokeWidth={3} /> : <Circle size={10} />}
                    </span>
                    As senhas coincidem
                  </li>
                </ul>

                <div className="profile-form-actions">
                  <button className="btn profile-primary-button" type="submit" disabled={!podeAlterarSenha}>
                    <KeyRound size={17} /> Alterar senha
                  </button>
                </div>
              </form>
            </section>
          </div>
        </div>
      </div>
    </main>
  )
}
