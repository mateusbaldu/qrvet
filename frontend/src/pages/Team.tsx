import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  CheckCircle2, Clock3, Mail, MoreVertical, Pencil, Search, Send, ShieldCheck,
  Stethoscope, Trash2, UserPlus, UserRound, Users, X, XCircle,
} from 'lucide-react'
import { useAuth } from '../auth/auth-context'
import { apiRequest, errorMessage, initials, type Role } from '../services/api'
import '../styles/team.css'

type Filtro = 'Todos' | 'Admins' | 'Veterinários' | 'Recepcionistas' | 'Pendentes'
type TipoAcao = 'permissao' | 'remover' | 'reenviar' | 'cancelar'
type Membro = {
  id: string; name: string; email: string; avatarUrl: string | null; role: Role
  status: 'ONLINE' | 'INACTIVE' | 'PENDING' | 'EXPIRED'; lastActivityAt: string | null
  createdAt: string; expiresAt: string | null; invitation: boolean
}
type TeamResponse = { items: Membro[]; summary: { total: number; roles: Record<Role, number>; pending: number } }

const filtros: Filtro[] = ['Todos', 'Admins', 'Veterinários', 'Recepcionistas', 'Pendentes']
const permissionLabels: Record<Role, string> = { ADMIN: 'Admin', VETERINARIAN: 'Veterinário', RECEPTIONIST: 'Recepcionista' }

function statusText(member: Membro) {
  if (member.status === 'PENDING') return 'Convite pendente'
  if (member.status === 'EXPIRED') return 'Convite expirado'
  if (member.status === 'ONLINE') return 'Online agora'
  if (!member.lastActivityAt) return 'Inativo'
  const minutes = Math.max(0, Math.floor((Date.now() - new Date(member.lastActivityAt).getTime()) / 60_000))
  if (minutes < 60) return `Inativo · Há ${Math.max(1, minutes)} min`
  if (minutes < 1_440) return `Inativo · Há ${Math.floor(minutes / 60)}h`
  return `Inativo · Há ${Math.floor(minutes / 1_440)}d`
}

function formattedDate(value: string) {
  return new Intl.DateTimeFormat('pt-BR').format(new Date(value))
}

export function Team() {
  const { user, updateUser } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const [membros, setMembros] = useState<Membro[]>([])
  const [busca, setBusca] = useState('')
  const [filtro, setFiltro] = useState<Filtro>('Todos')
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [modalAberto, setModalAberto] = useState(false)
  const [emailConvite, setEmailConvite] = useState('')
  const [cargoConvite, setCargoConvite] = useState<Role>('VETERINARIAN')
  const [menuAberto, setMenuAberto] = useState<string | null>(null)
  const [acaoAtual, setAcaoAtual] = useState<{ tipo: TipoAcao; membro: Membro } | null>(null)
  const [novaPermissao, setNovaPermissao] = useState<Role>('VETERINARIAN')
  const [processando, setProcessando] = useState(false)

  const carregarEquipe = useCallback(async () => {
    setErro('')
    try {
      const response = await apiRequest<TeamResponse>('/team/members')
      setMembros(response.items)
    } catch (error) {
      setErro(errorMessage(error))
    } finally {
      setCarregando(false)
    }
  }, [])

  useEffect(() => {
    let active = true
    const refreshPresence = () => {
      void apiRequest<TeamResponse>('/team/members')
        .then((response) => { if (active) setMembros(response.items) })
        .catch((error) => { if (active) setErro(errorMessage(error)) })
        .finally(() => { if (active) setCarregando(false) })
    }
    refreshPresence()
    const interval = window.setInterval(refreshPresence, 60_000)
    return () => { active = false; window.clearInterval(interval) }
  }, [])

  async function enviarConvite(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setProcessando(true); setErro('')
    try {
      await apiRequest('/team/invitations', { method: 'POST', body: JSON.stringify({ email: emailConvite, role: cargoConvite }) })
      setModalAberto(false); setEmailConvite(''); setCargoConvite('VETERINARIAN')
      setMensagem('Convite enviado. Ele já pode ser visualizado no Mailpit.')
      await carregarEquipe()
    } catch (error) { setErro(errorMessage(error)) } finally { setProcessando(false) }
  }

  function abrirAcao(tipo: TipoAcao, membro: Membro) {
    setMenuAberto(null); setErro(''); setMensagem(''); setNovaPermissao(membro.role); setAcaoAtual({ tipo, membro })
  }

  async function confirmarAcao(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!acaoAtual) return
    setProcessando(true); setErro('')
    try {
      if (acaoAtual.tipo === 'permissao') {
        await apiRequest(`/team/members/${acaoAtual.membro.id}/role`, { method: 'PATCH', body: JSON.stringify({ role: novaPermissao }) })
        if (user?.id === acaoAtual.membro.id) updateUser({ ...user, role: novaPermissao })
        setMensagem('Permissão alterada com sucesso.')
      } else if (acaoAtual.tipo === 'remover') {
        await apiRequest(`/team/members/${acaoAtual.membro.id}`, { method: 'DELETE' }); setMensagem('Membro removido da equipe.')
      } else if (acaoAtual.tipo === 'reenviar') {
        await apiRequest(`/team/invitations/${acaoAtual.membro.id}/resend`, { method: 'POST' }); setMensagem('Convite reenviado. Confira o Mailpit.')
      } else {
        await apiRequest(`/team/invitations/${acaoAtual.membro.id}`, { method: 'DELETE' }); setMensagem('Convite cancelado.')
      }
      setAcaoAtual(null); await carregarEquipe()
    } catch (error) { setErro(errorMessage(error)) } finally { setProcessando(false) }
  }

  const membrosFiltrados = useMemo(() => {
    const termo = busca.trim().toLocaleLowerCase('pt-BR')
    return membros.filter((membro) => {
      const correspondeBusca = !termo || `${membro.name} ${membro.email}`.toLocaleLowerCase('pt-BR').includes(termo)
      const correspondeFiltro = filtro === 'Todos'
        || (filtro === 'Admins' && membro.role === 'ADMIN' && !membro.invitation)
        || (filtro === 'Veterinários' && membro.role === 'VETERINARIAN' && !membro.invitation)
        || (filtro === 'Recepcionistas' && membro.role === 'RECEPTIONIST' && !membro.invitation)
        || (filtro === 'Pendentes' && membro.status === 'PENDING')
      return correspondeBusca && correspondeFiltro
    })
  }, [busca, filtro, membros])

  const count = (role: Role) => membros.filter((m) => m.role === role && !m.invitation).length
  const pending = membros.filter((m) => m.status === 'PENDING').length

  return <main className="team-page"><div className="team-container">
    <header className="team-heading"><div><h1>Equipe</h1><p>Gerencie quem tem acesso à clínica e suas permissões.</p></div>
      {isAdmin && <button className="btn team-invite" type="button" onClick={() => { setErro(''); setMensagem(''); setModalAberto(true) }}><UserPlus size={18} /> Convidar membro</button>}
    </header>
    {mensagem && <div className="api-feedback success" role="status">{mensagem}</div>}
    {erro && <div className="api-feedback error" role="alert">{erro} <button type="button" onClick={() => void carregarEquipe()}>Tentar novamente</button></div>}

    <section className="row g-3 team-stats" aria-label="Resumo da equipe">
      <StatCard label="Total" value={membros.length} active={filtro === 'Todos'} onClick={() => setFiltro('Todos')} icon={<Users size={16} />} />
      <StatCard label="Admins" value={count('ADMIN')} active={filtro === 'Admins'} onClick={() => setFiltro('Admins')} icon={<ShieldCheck size={16} />} kind="admin" />
      <StatCard label="Veterinários" value={count('VETERINARIAN')} active={filtro === 'Veterinários'} onClick={() => setFiltro('Veterinários')} icon={<Stethoscope size={16} />} kind="vet" />
      <StatCard label="Recepcionistas" value={count('RECEPTIONIST')} active={filtro === 'Recepcionistas'} onClick={() => setFiltro('Recepcionistas')} icon={<UserRound size={16} />} kind="receptionist" />
      <StatCard label="Pendentes" value={pending} active={filtro === 'Pendentes'} onClick={() => setFiltro('Pendentes')} icon={<Clock3 size={16} />} kind="pending" />
    </section>

    <section className="team-toolbar" aria-label="Busca e filtros">
      <label className="team-search" htmlFor="buscar-membro"><Search size={18} /><input id="buscar-membro" type="search" value={busca} onChange={(event) => setBusca(event.target.value)} placeholder="Buscar por nome ou e-mail..." /></label>
      <div className="team-filters" role="group" aria-label="Filtrar membros">{filtros.map((opcao) => <button className={filtro === opcao ? 'active' : ''} type="button" key={opcao} onClick={() => setFiltro(opcao)} aria-pressed={filtro === opcao}>{opcao}</button>)}</div>
    </section>

    <section className="team-table-card" aria-label="Membros da equipe">
      {carregando ? <div className="team-empty"><span className="spinner-border" /><strong>Carregando equipe...</strong></div> : <div className="table-responsive"><table className="table team-table align-middle mb-0">
        <thead><tr><th>Membro</th><th>Permissão</th><th>Status</th><th>Adicionado em</th><th><span className="visually-hidden">Ações</span></th></tr></thead>
        <tbody>{membrosFiltrados.map((membro) => <tr key={membro.id} className={membro.invitation ? 'member-pending' : ''}>
          <td><div className="member-cell"><span className={`member-avatar ${membro.invitation ? 'pending' : ''}`}>{membro.invitation ? <Mail size={16} /> : membro.avatarUrl ? <img src={membro.avatarUrl} alt="" /> : initials(membro.name)}</span><span><strong>{membro.name}</strong><small>{membro.email}</small></span></div></td>
          <td><PermissionBadge role={membro.role} /></td>
          <td><span className={`status-badge ${membro.status === 'PENDING' || membro.status === 'EXPIRED' ? 'pending' : membro.status === 'INACTIVE' ? 'inactive' : ''}`}>{membro.status === 'PENDING' || membro.status === 'EXPIRED' ? <Clock3 size={13} /> : membro.status === 'INACTIVE' ? <XCircle size={13} /> : <CheckCircle2 size={13} />}{statusText(membro)}</span></td>
          <td className="member-date">{formattedDate(membro.createdAt)}</td>
          <td className="text-end member-actions-cell">{isAdmin && <><button className="member-actions" type="button" aria-label={`Ações de ${membro.name}`} onClick={() => setMenuAberto((atual) => atual === membro.id ? null : membro.id)}><MoreVertical size={18} /></button>
            {menuAberto === membro.id && <div className="member-menu" role="menu">{membro.invitation ? <><button type="button" onClick={() => abrirAcao('reenviar', membro)}><Send size={15} /> Reenviar convite</button>{membro.status === 'PENDING' && <button type="button" className="danger" onClick={() => abrirAcao('cancelar', membro)}><XCircle size={15} /> Cancelar convite</button>}</> : <><button type="button" onClick={() => abrirAcao('permissao', membro)}><Pencil size={15} /> Alterar permissão</button><button type="button" className="danger" onClick={() => abrirAcao('remover', membro)}><Trash2 size={15} /> Remover da equipe</button></>}</div>}</>}</td>
        </tr>)}</tbody>
      </table></div>}
      {!carregando && membrosFiltrados.length === 0 && <div className="team-empty"><Search size={25} /><strong>Nenhum membro encontrado</strong><span>Tente alterar sua busca ou selecionar outro filtro.</span></div>}
    </section>
  </div>

  {modalAberto && <div className="team-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setModalAberto(false) }}><section className="team-modal" role="dialog" aria-modal="true">
    <button className="team-modal-close" onClick={() => setModalAberto(false)} aria-label="Fechar"><X size={20} /></button><span className="team-modal-icon"><UserPlus size={23} /></span><h2>Convidar membro</h2><p>O profissional receberá um convite para criar a conta.</p>{erro && <div className="api-feedback error" role="alert">{erro}</div>}
    <form onSubmit={enviarConvite}><label className="form-label auth-label" htmlFor="email-convite">E-mail do membro</label><input className="form-control auth-input" id="email-convite" type="email" value={emailConvite} onChange={(event) => setEmailConvite(event.target.value)} required autoFocus />
      <label className="form-label auth-label mt-3" htmlFor="cargo-convite">Permissão inicial</label><select className="form-select profile-input" id="cargo-convite" value={cargoConvite} onChange={(event) => setCargoConvite(event.target.value as Role)}><option value="VETERINARIAN">Veterinário</option><option value="RECEPTIONIST">Recepcionista</option><option value="ADMIN">Administrador</option></select>
      <div className="team-modal-actions"><button className="btn team-modal-cancel" type="button" onClick={() => setModalAberto(false)}>Cancelar</button><button className="btn team-invite" disabled={processando}>{processando ? 'Enviando...' : <><Send size={17} /> Enviar convite</>}</button></div>
    </form></section></div>}

  {acaoAtual && <div className="team-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setAcaoAtual(null) }}><section className={`team-modal ${acaoAtual.tipo === 'remover' || acaoAtual.tipo === 'cancelar' ? 'team-modal-danger' : ''}`} role="dialog" aria-modal="true">
    <button className="team-modal-close" onClick={() => setAcaoAtual(null)} aria-label="Fechar"><X size={20} /></button><span className="team-modal-icon">{acaoAtual.tipo === 'permissao' && <Pencil size={22} />}{acaoAtual.tipo === 'remover' && <Trash2 size={22} />}{acaoAtual.tipo === 'reenviar' && <Send size={22} />}{acaoAtual.tipo === 'cancelar' && <XCircle size={22} />}</span>
    <h2>{acaoAtual.tipo === 'permissao' ? 'Alterar permissão' : acaoAtual.tipo === 'remover' ? 'Remover da equipe?' : acaoAtual.tipo === 'reenviar' ? 'Reenviar convite?' : 'Cancelar convite?'}</h2>
    <p>{acaoAtual.tipo === 'permissao' ? `Escolha o nível de acesso de ${acaoAtual.membro.name}.` : acaoAtual.tipo === 'remover' ? `${acaoAtual.membro.name} perderá o acesso à clínica.` : acaoAtual.tipo === 'reenviar' ? `Um novo convite será enviado para ${acaoAtual.membro.email}.` : `O convite de ${acaoAtual.membro.email} deixará de ser válido.`}</p>{erro && <div className="api-feedback error" role="alert">{erro}</div>}
    <form onSubmit={confirmarAcao}>{acaoAtual.tipo === 'permissao' && <fieldset className="permission-options"><legend>Permissão do membro</legend>{(['VETERINARIAN', 'ADMIN', 'RECEPTIONIST'] as Role[]).map((role) => <label className={novaPermissao === role ? 'selected' : ''} key={role}><input type="radio" name="permissao" checked={novaPermissao === role} onChange={() => setNovaPermissao(role)} /><span className="permission-option-icon">{role === 'ADMIN' ? <ShieldCheck size={18} /> : role === 'VETERINARIAN' ? <Stethoscope size={18} /> : <UserRound size={18} />}</span><span><strong>{permissionLabels[role]}</strong><small>{role === 'ADMIN' ? 'Gerencia equipe, permissões e configurações.' : role === 'VETERINARIAN' ? 'Acesso às rotinas clínicas e aos pacientes.' : 'Acesso ao atendimento e às rotinas administrativas.'}</small></span></label>)}</fieldset>}
      <div className="team-modal-actions"><button className="btn team-modal-cancel" type="button" onClick={() => setAcaoAtual(null)}>Cancelar</button><button className={`btn ${acaoAtual.tipo === 'remover' || acaoAtual.tipo === 'cancelar' ? 'team-danger-button' : 'team-invite'}`} disabled={processando || (acaoAtual.tipo === 'permissao' && novaPermissao === acaoAtual.membro.role)}>{processando ? 'Processando...' : acaoAtual.tipo === 'permissao' ? 'Salvar alteração' : acaoAtual.tipo === 'remover' ? 'Remover membro' : acaoAtual.tipo === 'reenviar' ? 'Reenviar convite' : 'Cancelar convite'}</button></div>
    </form></section></div>}
  </main>
}

function StatCard({ label, value, active, onClick, icon, kind = '' }: { label: string; value: number; active: boolean; onClick: () => void; icon: React.ReactNode; kind?: string }) {
  return <div className="col-6 col-xl"><button type="button" className={`team-stat-card ${kind} ${active ? 'active' : ''}`} onClick={onClick} aria-pressed={active}><span>{icon}{label}</span><strong>{value}</strong></button></div>
}

function PermissionBadge({ role }: { role: Role }) {
  return <span className={`permission-badge ${role === 'ADMIN' ? 'admin' : role === 'VETERINARIAN' ? 'vet' : 'receptionist'}`}>{role === 'ADMIN' ? <ShieldCheck size={13} /> : role === 'VETERINARIAN' ? <Stethoscope size={13} /> : <UserRound size={13} />}{permissionLabels[role]}</span>
}
