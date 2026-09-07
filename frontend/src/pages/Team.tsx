import { useMemo, useState } from 'react'
import {
  CheckCircle2,
  Clock3,
  Mail,
  MoreVertical,
  Pencil,
  Search,
  Send,
  ShieldCheck,
  Stethoscope,
  Trash2,
  UserRound,
  UserPlus,
  Users,
  X,
  XCircle,
} from 'lucide-react'
import '../styles/team.css'

type Filtro = 'Todos' | 'Admins' | 'Veterinários' | 'Recepcionistas' | 'Pendentes'
type TipoAcao = 'permissao' | 'remover' | 'reenviar' | 'cancelar'

type Membro = {
  nome: string
  email: string
  iniciais: string
  permissao: 'Admin' | 'Veterinário' | 'Recepcionista'
  status: string
  pendente?: boolean
  data: string
}

const membros: Membro[] = [
  { nome: 'Dra. Sarah Jenkins', email: 'sarah@qrvet.clinic', iniciais: 'SJ', permissao: 'Admin', status: 'Ativo · Agora', data: '14/01/2026' },
  { nome: 'Mike Ross', email: 'mike@qrvet.clinic', iniciais: 'MR', permissao: 'Veterinário', status: 'Ativo · Há 2h', data: '02/02/2026' },
  { nome: 'Anna Costa', email: 'anna@qrvet.clinic', iniciais: 'AC', permissao: 'Veterinário', status: 'Inativo', data: '11/03/2026' },
  { nome: 'Camila Alves', email: 'camila@qrvet.clinic', iniciais: 'CA', permissao: 'Recepcionista', status: 'Ativo · Há 35 min', data: '08/04/2026' },
  { nome: 'Aguardando aceite', email: 'lucas@qrvet.clinic', iniciais: 'LC', permissao: 'Veterinário', status: 'Convite pendente', pendente: true, data: '17/05/2026' },
]

const filtros: Filtro[] = ['Todos', 'Admins', 'Veterinários', 'Recepcionistas', 'Pendentes']

export function Team() {
  const [busca, setBusca] = useState('')
  const [filtro, setFiltro] = useState<Filtro>('Todos')
  const [modalAberto, setModalAberto] = useState(false)
  const [emailConvite, setEmailConvite] = useState('')
  const [menuAberto, setMenuAberto] = useState<string | null>(null)
  const [acaoAtual, setAcaoAtual] = useState<{ tipo: TipoAcao; membro: Membro } | null>(null)
  const [novaPermissao, setNovaPermissao] = useState<Membro['permissao']>('Veterinário')

  function enviarConvite(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setModalAberto(false)
    setEmailConvite('')
  }

  function abrirAcao(tipo: TipoAcao, membro: Membro) {
    setMenuAberto(null)
    setNovaPermissao(membro.permissao)
    setAcaoAtual({ tipo, membro })
  }

  function confirmarAcao(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setAcaoAtual(null)
  }

  const membrosFiltrados = useMemo(() => {
    const termo = busca.trim().toLocaleLowerCase('pt-BR')

    return membros.filter((membro) => {
      const correspondeBusca = !termo || `${membro.nome} ${membro.email}`.toLocaleLowerCase('pt-BR').includes(termo)
      const correspondeFiltro =
        filtro === 'Todos' ||
        (filtro === 'Admins' && membro.permissao === 'Admin') ||
        (filtro === 'Veterinários' && membro.permissao === 'Veterinário' && !membro.pendente) ||
        (filtro === 'Recepcionistas' && membro.permissao === 'Recepcionista' && !membro.pendente) ||
        (filtro === 'Pendentes' && membro.pendente)

      return correspondeBusca && correspondeFiltro
    })
  }, [busca, filtro])

  return (
    <main className="team-page">
      <div className="team-container">
        <header className="team-heading">
          <div>
            <h1>Equipe</h1>
            <p>Gerencie quem tem acesso à clínica e suas permissões.</p>
          </div>
          <button className="btn team-invite" type="button" onClick={() => setModalAberto(true)}>
            <UserPlus size={18} /> Convidar membro
          </button>
        </header>

        <section className="row g-3 team-stats" aria-label="Resumo da equipe">
          <div className="col-6 col-xl">
            <button className={`team-stat-card ${filtro === 'Todos' ? 'active' : ''}`} type="button" onClick={() => setFiltro('Todos')} aria-pressed={filtro === 'Todos'}>
              <span><Users size={16} /> Total</span><strong>{membros.length}</strong>
            </button>
          </div>
          <div className="col-6 col-xl">
            <button className={`team-stat-card admin ${filtro === 'Admins' ? 'active' : ''}`} type="button" onClick={() => setFiltro('Admins')} aria-pressed={filtro === 'Admins'}>
              <span><ShieldCheck size={16} /> Admins</span><strong>{membros.filter((membro) => membro.permissao === 'Admin' && !membro.pendente).length}</strong>
            </button>
          </div>
          <div className="col-6 col-xl">
            <button className={`team-stat-card vet ${filtro === 'Veterinários' ? 'active' : ''}`} type="button" onClick={() => setFiltro('Veterinários')} aria-pressed={filtro === 'Veterinários'}>
              <span><Stethoscope size={16} /> Veterinários</span><strong>{membros.filter((membro) => membro.permissao === 'Veterinário' && !membro.pendente).length}</strong>
            </button>
          </div>
          <div className="col-6 col-xl">
            <button className={`team-stat-card receptionist ${filtro === 'Recepcionistas' ? 'active' : ''}`} type="button" onClick={() => setFiltro('Recepcionistas')} aria-pressed={filtro === 'Recepcionistas'}>
              <span><UserRound size={16} /> Recepcionistas</span><strong>{membros.filter((membro) => membro.permissao === 'Recepcionista' && !membro.pendente).length}</strong>
            </button>
          </div>
          <div className="col-6 col-xl">
            <button className={`team-stat-card pending ${filtro === 'Pendentes' ? 'active' : ''}`} type="button" onClick={() => setFiltro('Pendentes')} aria-pressed={filtro === 'Pendentes'}>
              <span><Clock3 size={16} /> Pendentes</span><strong>{membros.filter((membro) => membro.pendente).length}</strong>
            </button>
          </div>
        </section>

        <section className="team-toolbar" aria-label="Busca e filtros">
          <label className="team-search" htmlFor="buscar-membro">
            <Search size={18} aria-hidden="true" />
            <input
              id="buscar-membro"
              type="search"
              value={busca}
              onChange={(event) => setBusca(event.target.value)}
              placeholder="Buscar por nome ou e-mail..."
            />
          </label>
          <div className="team-filters" role="group" aria-label="Filtrar membros">
            {filtros.map((opcao) => (
              <button
                className={filtro === opcao ? 'active' : ''}
                type="button"
                key={opcao}
                onClick={() => setFiltro(opcao)}
                aria-pressed={filtro === opcao}
              >
                {opcao}
              </button>
            ))}
          </div>
        </section>

        <section className="team-table-card" aria-label="Membros da equipe">
          <div className="table-responsive">
            <table className="table team-table align-middle mb-0">
              <thead>
                <tr>
                  <th scope="col">Membro</th>
                  <th scope="col">Permissão</th>
                  <th scope="col">Status</th>
                  <th scope="col">Adicionado em</th>
                  <th scope="col"><span className="visually-hidden">Ações</span></th>
                </tr>
              </thead>
              <tbody>
                {membrosFiltrados.map((membro) => (
                  <tr key={membro.email} className={membro.pendente ? 'member-pending' : ''}>
                    <td>
                      <div className="member-cell">
                        <span className={`member-avatar ${membro.pendente ? 'pending' : ''}`}>
                          {membro.pendente ? <Mail size={16} /> : membro.iniciais}
                        </span>
                        <span><strong>{membro.nome}</strong><small>{membro.email}</small></span>
                      </div>
                    </td>
                    <td>
                      <span className={`permission-badge ${membro.permissao === 'Admin' ? 'admin' : membro.permissao === 'Veterinário' ? 'vet' : 'receptionist'}`}>
                        {membro.permissao === 'Admin' ? <ShieldCheck size={13} /> : membro.permissao === 'Veterinário' ? <Stethoscope size={13} /> : <UserRound size={13} />}
                        {membro.permissao}
                      </span>
                    </td>
                    <td>
                      <span className={`status-badge ${membro.pendente ? 'pending' : membro.status === 'Inativo' ? 'inactive' : ''}`}>
                        {membro.pendente ? <Clock3 size={13} /> : membro.status === 'Inativo' ? <XCircle size={13} /> : <CheckCircle2 size={13} />}
                      </span>
                    </td>
                    <td className="member-date">{membro.data}</td>
                    <td className="text-end member-actions-cell">
                      <button
                        className="member-actions"
                        type="button"
                        aria-label={`Ações de ${membro.nome}`}
                        aria-expanded={menuAberto === membro.email}
                        aria-controls={`menu-${membro.email}`}
                        onClick={() => setMenuAberto((atual) => atual === membro.email ? null : membro.email)}
                      >
                        <MoreVertical size={18} />
                      </button>
                      {menuAberto === membro.email && (
                        <div className="member-menu" id={`menu-${membro.email}`} role="menu">
                          {membro.pendente ? (
                            <>
                              <button type="button" role="menuitem" onClick={() => abrirAcao('reenviar', membro)}>
                                <Send size={15} /> Reenviar convite
                              </button>
                              <button type="button" role="menuitem" className="danger" onClick={() => abrirAcao('cancelar', membro)}>
                                <XCircle size={15} /> Cancelar convite
                              </button>
                            </>
                          ) : (
                            <>
                              <button type="button" role="menuitem" onClick={() => abrirAcao('permissao', membro)}>
                                <Pencil size={15} /> Alterar permissão
                              </button>
                              <button type="button" role="menuitem" className="danger" onClick={() => abrirAcao('remover', membro)}>
                                <Trash2 size={15} /> Remover da equipe
                              </button>
                            </>
                          )}
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {membrosFiltrados.length === 0 && (
            <div className="team-empty">
              <Search size={25} />
              <strong>Nenhum membro encontrado</strong>
              <span>Tente alterar sua busca ou selecionar outro filtro.</span>
            </div>
          )}
        </section>
      </div>

      {modalAberto && (
        <div
          className="team-modal-backdrop"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) setModalAberto(false)
          }}
        >
          <section className="team-modal" role="dialog" aria-modal="true" aria-labelledby="invite-title">
            <button className="team-modal-close" type="button" onClick={() => setModalAberto(false)} aria-label="Fechar">
              <X size={20} />
            </button>
            <span className="team-modal-icon"><UserPlus size={23} /></span>
            <h2 id="invite-title">Convidar membro</h2>
            <p>Enviaremos um convite para o profissional fazer parte da equipe da clínica.</p>

            <form onSubmit={enviarConvite}>
              <label className="form-label auth-label" htmlFor="email-convite">E-mail do membro</label>
              <input
                className="form-control auth-input"
                id="email-convite"
                type="email"
                value={emailConvite}
                onChange={(event) => setEmailConvite(event.target.value)}
                placeholder="exemplo@exemplo.com"
                autoComplete="email"
                autoFocus
                required
              />
              <div className="team-modal-actions">
                <button className="btn team-modal-cancel" type="button" onClick={() => setModalAberto(false)}>Cancelar</button>
                <button className="btn team-invite" type="submit"><Send size={17} /> Enviar convite</button>
              </div>
            </form>
          </section>
        </div>
      )}

      {acaoAtual && (
        <div
          className="team-modal-backdrop"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) setAcaoAtual(null)
          }}
        >
          <section
            className={`team-modal ${acaoAtual.tipo === 'remover' || acaoAtual.tipo === 'cancelar' ? 'team-modal-danger' : ''}`}
            role="dialog"
            aria-modal="true"
            aria-labelledby="member-action-title"
          >
            <button className="team-modal-close" type="button" onClick={() => setAcaoAtual(null)} aria-label="Fechar">
              <X size={20} />
            </button>

            <span className="team-modal-icon">
              {acaoAtual.tipo === 'permissao' && <Pencil size={22} />}
              {acaoAtual.tipo === 'remover' && <Trash2 size={22} />}
              {acaoAtual.tipo === 'reenviar' && <Send size={22} />}
              {acaoAtual.tipo === 'cancelar' && <XCircle size={22} />}
            </span>

            <h2 id="member-action-title">
              {acaoAtual.tipo === 'permissao' && 'Alterar permissão'}
              {acaoAtual.tipo === 'remover' && 'Remover da equipe?'}
              {acaoAtual.tipo === 'reenviar' && 'Reenviar convite?'}
              {acaoAtual.tipo === 'cancelar' && 'Cancelar convite?'}
            </h2>

            <p>
              {acaoAtual.tipo === 'permissao' && `Escolha o nível de acesso de ${acaoAtual.membro.nome}.`}
              {acaoAtual.tipo === 'remover' && `${acaoAtual.membro.nome} perderá o acesso à clínica e aos dados da equipe.`}
              {acaoAtual.tipo === 'reenviar' && `Um novo convite será enviado para ${acaoAtual.membro.email}.`}
              {acaoAtual.tipo === 'cancelar' && `O convite enviado para ${acaoAtual.membro.email} deixará de ser válido.`}
            </p>

            <form onSubmit={confirmarAcao}>
              {acaoAtual.tipo === 'permissao' && (
                <fieldset className="permission-options">
                  <legend>Permissão do membro</legend>
                  <label className={novaPermissao === 'Veterinário' ? 'selected' : ''}>
                    <input
                      type="radio"
                      name="permissao"
                      value="Veterinário"
                      checked={novaPermissao === 'Veterinário'}
                      onChange={() => setNovaPermissao('Veterinário')}
                    />
                    <span className="permission-option-icon"><Stethoscope size={18} /></span>
                    <span><strong>Veterinário</strong><small>Acesso às rotinas clínicas e aos pacientes.</small></span>
                  </label>
                  <label className={novaPermissao === 'Admin' ? 'selected' : ''}>
                    <input
                      type="radio"
                      name="permissao"
                      value="Admin"
                      checked={novaPermissao === 'Admin'}
                      onChange={() => setNovaPermissao('Admin')}
                    />
                    <span className="permission-option-icon"><ShieldCheck size={18} /></span>
                    <span><strong>Administrador</strong><small>Gerencia equipe, permissões e configurações.</small></span>
                  </label>
                  <label className={novaPermissao === 'Recepcionista' ? 'selected' : ''}>
                    <input
                      type="radio"
                      name="permissao"
                      value="Recepcionista"
                      checked={novaPermissao === 'Recepcionista'}
                      onChange={() => setNovaPermissao('Recepcionista')}
                    />
                    <span className="permission-option-icon"><UserRound size={18} /></span>
                    <span><strong>Recepcionista</strong><small>Acesso ao atendimento e às rotinas administrativas.</small></span>
                  </label>
                </fieldset>
              )}

              <div className="team-modal-actions">
                <button className="btn team-modal-cancel" type="button" onClick={() => setAcaoAtual(null)}>Cancelar</button>
                <button
                  className={`btn ${acaoAtual.tipo === 'remover' || acaoAtual.tipo === 'cancelar' ? 'team-danger-button' : 'team-invite'}`}
                  type="submit"
                  disabled={acaoAtual.tipo === 'permissao' && novaPermissao === acaoAtual.membro.permissao}
                >
                  {acaoAtual.tipo === 'permissao' && 'Salvar alteração'}
                  {acaoAtual.tipo === 'remover' && 'Remover membro'}
                  {acaoAtual.tipo === 'reenviar' && <><Send size={17} /> Reenviar convite</>}
                  {acaoAtual.tipo === 'cancelar' && 'Cancelar convite'}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </main>
  )
}
