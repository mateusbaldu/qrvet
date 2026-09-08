import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { CheckCircle2, Clock3, Mail, MoreVertical, Search, Send, ShieldCheck, Stethoscope, UserRound, UserPlus, Users, X, XCircle } from 'lucide-react'
import { readableError, usersApi, type Role, type User } from '../services/api'
import '../styles/team.css'

type Filtro = 'Todos' | 'Admins' | 'Veterinários' | 'Recepcionistas' | 'Pendentes'
const filtros: Filtro[] = ['Todos', 'Admins', 'Veterinários', 'Recepcionistas', 'Pendentes']
const roleLabels: Record<Role, string> = { ADMIN: 'Admin', VETERINARIO: 'Veterinário', RECEPCIONISTA: 'Recepcionista', AUXILIAR_TECNICO: 'Auxiliar técnico', TUTOR: 'Tutor' }

function initials(name: string) {
  return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase()).join('') || '—'
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('pt-BR').format(new Date(value))
}

function RoleBadge({ role }: { role: Role }) {
  const css = role === 'ADMIN' ? 'admin' : role === 'VETERINARIO' ? 'vet' : 'receptionist'
  const icon = role === 'ADMIN' ? <ShieldCheck size={13} /> : role === 'VETERINARIO' ? <Stethoscope size={13} /> : <UserRound size={13} />
  return <span className={`permission-badge ${css}`}>{icon}{roleLabels[role]}</span>
}

function presence(member: User) {
  if (!member.confirmed) return { label: 'Convite pendente', css: 'pending', icon: <Clock3 size={13} /> }
  if (!member.active) return { label: 'Conta inativa', css: 'inactive', icon: <XCircle size={13} /> }
  if (!member.lastActivityAt) return { label: 'Nunca acessou', css: 'inactive', icon: <XCircle size={13} /> }

  const minutes = Math.max(0, Math.floor((Date.now() - new Date(member.lastActivityAt).getTime()) / 60_000))
  if (minutes < 2) return { label: 'Ativo agora', css: '', icon: <CheckCircle2 size={13} /> }
  if (minutes < 60) return { label: `Inativo · há ${minutes} min`, css: 'inactive', icon: <XCircle size={13} /> }
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return { label: `Inativo · há ${hours}h`, css: 'inactive', icon: <XCircle size={13} /> }
  const days = Math.floor(hours / 24)
  return { label: `Inativo · há ${days}d`, css: 'inactive', icon: <XCircle size={13} /> }
}

export function Team() {
  const [members, setMembers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState<Filtro>('Todos')
  const [inviteOpen, setInviteOpen] = useState(false)
  const [inviteName, setInviteName] = useState('')
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteRole, setInviteRole] = useState<Role>('VETERINARIO')
  const [submitting, setSubmitting] = useState(false)
  const [openMenu, setOpenMenu] = useState<number | null>(null)
  const [resending, setResending] = useState<User | null>(null)

  useEffect(() => {
    let active = true
    const refresh = () => {
      void usersApi.list()
        .then((page) => {
          if (!active) return
          setError('')
          setMembers(page.items)
        })
        .catch((requestError) => { if (active) setError(readableError(requestError)) })
        .finally(() => { if (active) setLoading(false) })
    }
    refresh()
    const interval = window.setInterval(refresh, 60_000)
    return () => { active = false; window.clearInterval(interval) }
  }, [])

  async function sendInvite(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true); setError(''); setSuccess('')
    try {
      const created = await usersApi.create({ name: inviteName, email: inviteEmail, role: inviteRole })
      setMembers((current) => [created, ...current])
      setSuccess(`Convite enviado para ${created.email}.`)
      setInviteOpen(false); setInviteName(''); setInviteEmail(''); setInviteRole('VETERINARIO')
    } catch (requestError) {
      setError(readableError(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  async function resendInvite(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!resending) return
    setSubmitting(true); setError(''); setSuccess('')
    try {
      await usersApi.resendInvitation(resending.id)
      setSuccess(`Um novo convite foi enviado para ${resending.email}.`)
      setResending(null)
    } catch (requestError) {
      setError(readableError(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  const filteredMembers = useMemo(() => {
    const term = search.trim().toLocaleLowerCase('pt-BR')
    return members.filter((member) => {
      const pending = !member.confirmed
      const matchesSearch = !term || `${member.name} ${member.email}`.toLocaleLowerCase('pt-BR').includes(term)
      const matchesFilter = filter === 'Todos'
        || (filter === 'Admins' && member.role === 'ADMIN' && !pending)
        || (filter === 'Veterinários' && member.role === 'VETERINARIO' && !pending)
        || (filter === 'Recepcionistas' && member.role === 'RECEPCIONISTA' && !pending)
        || (filter === 'Pendentes' && pending)
      return matchesSearch && matchesFilter
    })
  }, [filter, members, search])
  const count = (predicate: (member: User) => boolean) => members.filter(predicate).length

  return <main className="team-page">
    <div className="team-container">
      <header className="team-heading"><div><h1>Equipe</h1><p>Gerencie quem tem acesso à clínica e suas permissões.</p></div><button className="btn team-invite" type="button" onClick={() => setInviteOpen(true)}><UserPlus size={18} /> Convidar membro</button></header>
      {(error || success) && <p className={`form-message ${error ? 'error' : 'success'}`} role="alert">{error || success}</p>}

      <section className="row g-3 team-stats" aria-label="Resumo da equipe">
        <div className="col-6 col-xl"><button className={`team-stat-card ${filter === 'Todos' ? 'active' : ''}`} type="button" onClick={() => setFilter('Todos')}><span><Users size={16} /> Total</span><strong>{members.length}</strong></button></div>
        <div className="col-6 col-xl"><button className={`team-stat-card admin ${filter === 'Admins' ? 'active' : ''}`} type="button" onClick={() => setFilter('Admins')}><span><ShieldCheck size={16} /> Admins</span><strong>{count((m) => m.role === 'ADMIN' && m.confirmed)}</strong></button></div>
        <div className="col-6 col-xl"><button className={`team-stat-card vet ${filter === 'Veterinários' ? 'active' : ''}`} type="button" onClick={() => setFilter('Veterinários')}><span><Stethoscope size={16} /> Veterinários</span><strong>{count((m) => m.role === 'VETERINARIO' && m.confirmed)}</strong></button></div>
        <div className="col-6 col-xl"><button className={`team-stat-card receptionist ${filter === 'Recepcionistas' ? 'active' : ''}`} type="button" onClick={() => setFilter('Recepcionistas')}><span><UserRound size={16} /> Recepcionistas</span><strong>{count((m) => m.role === 'RECEPCIONISTA' && m.confirmed)}</strong></button></div>
        <div className="col-6 col-xl"><button className={`team-stat-card pending ${filter === 'Pendentes' ? 'active' : ''}`} type="button" onClick={() => setFilter('Pendentes')}><span><Clock3 size={16} /> Pendentes</span><strong>{count((m) => !m.confirmed)}</strong></button></div>
      </section>

      <section className="team-toolbar" aria-label="Busca e filtros"><label className="team-search" htmlFor="buscar-membro"><Search size={18} /><input id="buscar-membro" type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar por nome ou e-mail..." /></label><div className="team-filters" role="group" aria-label="Filtrar membros">{filtros.map((option) => <button className={filter === option ? 'active' : ''} type="button" key={option} onClick={() => setFilter(option)}>{option}</button>)}</div></section>

      <section className="team-table-card" aria-label="Membros da equipe">
        {loading ? <div className="team-empty"><strong>Carregando equipe...</strong></div> : <div className="table-responsive"><table className="table team-table align-middle mb-0"><thead><tr><th>Membro</th><th>Permissão</th><th>Status da conta</th><th>Adicionado em</th><th><span className="visually-hidden">Ações</span></th></tr></thead><tbody>{filteredMembers.map((member) => {
          const pending = !member.confirmed
          const memberPresence = presence(member)
          return <tr key={member.id} className={pending ? 'member-pending' : ''}>
            <td><div className="member-cell"><span className={`member-avatar ${pending ? 'pending' : ''}`}>{pending ? <Mail size={16} /> : initials(member.name)}</span><span><strong>{member.name}</strong><small>{member.email}</small></span></div></td>
            <td><RoleBadge role={member.role} /></td>
            <td><span className={`status-badge ${memberPresence.css}`}>{memberPresence.icon}{memberPresence.label}</span></td>
            <td className="member-date">{formatDate(member.createdAt)}</td>
            <td className="text-end member-actions-cell">{pending && <><button className="member-actions" type="button" aria-label={`Ações de ${member.name}`} onClick={() => setOpenMenu((current) => current === member.id ? null : member.id)}><MoreVertical size={18} /></button>{openMenu === member.id && <div className="member-menu" role="menu"><button type="button" role="menuitem" onClick={() => { setOpenMenu(null); setResending(member) }}><Send size={15} /> Reenviar convite</button></div>}</>}</td>
          </tr>
        })}</tbody></table></div>}
        {!loading && filteredMembers.length === 0 && <div className="team-empty"><Search size={25} /><strong>Nenhum membro encontrado</strong><span>Tente alterar sua busca ou selecionar outro filtro.</span></div>}
      </section>
    </div>

    {inviteOpen && <div className="team-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setInviteOpen(false) }}><section className="team-modal" role="dialog" aria-modal="true" aria-labelledby="invite-title"><button className="team-modal-close" type="button" onClick={() => setInviteOpen(false)} aria-label="Fechar"><X size={20} /></button><span className="team-modal-icon"><UserPlus size={23} /></span><h2 id="invite-title">Convidar membro</h2><p>Enviaremos um convite para o profissional escolher a senha e ativar a conta.</p><form onSubmit={sendInvite}>
      <label className="form-label auth-label" htmlFor="nome-convite">Nome completo</label><input className="form-control auth-input mb-3" id="nome-convite" value={inviteName} onChange={(event) => setInviteName(event.target.value)} maxLength={120} autoFocus required />
      <label className="form-label auth-label" htmlFor="email-convite">E-mail</label><input className="form-control auth-input mb-3" id="email-convite" type="email" value={inviteEmail} onChange={(event) => setInviteEmail(event.target.value)} autoComplete="email" required />
      <label className="form-label auth-label" htmlFor="permissao-convite">Permissão</label><select className="form-select auth-input" id="permissao-convite" value={inviteRole} onChange={(event) => setInviteRole(event.target.value as Role)}><option value="VETERINARIO">Veterinário</option><option value="RECEPCIONISTA">Recepcionista</option><option value="ADMIN">Administrador</option><option value="AUXILIAR_TECNICO">Auxiliar técnico</option></select>
      <div className="team-modal-actions"><button className="btn team-modal-cancel" type="button" onClick={() => setInviteOpen(false)}>Cancelar</button><button className="btn team-invite" type="submit" disabled={submitting}>{submitting ? 'Enviando...' : <><Send size={17} /> Enviar convite</>}</button></div>
    </form></section></div>}

    {resending && <div className="team-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setResending(null) }}><section className="team-modal" role="dialog" aria-modal="true" aria-labelledby="resend-title"><button className="team-modal-close" type="button" onClick={() => setResending(null)} aria-label="Fechar"><X size={20} /></button><span className="team-modal-icon"><Send size={22} /></span><h2 id="resend-title">Reenviar convite?</h2><p>Um novo convite será enviado para {resending.email}.</p><form onSubmit={resendInvite}><div className="team-modal-actions"><button className="btn team-modal-cancel" type="button" onClick={() => setResending(null)}>Cancelar</button><button className="btn team-invite" type="submit" disabled={submitting}>{submitting ? 'Enviando...' : 'Reenviar convite'}</button></div></form></section></div>}
  </main>
}
