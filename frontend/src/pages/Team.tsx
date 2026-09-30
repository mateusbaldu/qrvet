import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Mail, Search, Send, ShieldCheck, UserPlus, Users } from 'lucide-react'
import { useResource } from '../hooks/useResource'
import { Editor, EmptyState, Notice, Page, Pagination, ResourceState } from '../components/ClinicUI'
import { usersApi, type Role, type User } from '../services/api'
import { allPages, dateTime, paginate, searchText } from '../services/clinic'

const roleLabels: Record<Role, string> = { ADMIN: 'Administrador', VETERINARIO: 'Veterinário', RECEPCIONISTA: 'Recepcionista', AUXILIAR_TECNICO: 'Auxiliar técnico', TUTOR: 'Tutor' }
const filters = [['', 'Todos'], ['ADMIN', 'Admins'], ['VETERINARIO', 'Veterinários'], ['RECEPCIONISTA', 'Recepcionistas'], ['AUXILIAR_TECNICO', 'Auxiliares'], ['TUTOR', 'Tutores'], ['PENDING', 'Pendentes']]
const initials = (name: string) => name.trim().split(/\s+/).slice(0, 2).map(part => part[0]?.toUpperCase()).join('')

export function Team() {
  const resource = useResource(() => allPages<User>('/users'), 'team')
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('')
  const [page, setPage] = useState(0)
  const [invite, setInvite] = useState(false)
  const [resending, setResending] = useState<User | null>(null)
  const [success, setSuccess] = useState('')
  const members = resource.data ?? []
  const filtered = members.filter(member => searchText(member.name + ' ' + member.email).includes(searchText(search.trim())) && (!filter || (filter === 'PENDING' ? !member.confirmed : member.role === filter)))
  const result = paginate(filtered, page)
  return <Page title="Equipe" description="Gerencie quem tem acesso à clínica e acompanhe seus convites." action={<button className="clinic-primary" onClick={() => setInvite(true)}><UserPlus size={17} />Convidar membro</button>}>
    {success && <Notice>{success}</Notice>}
    <div className="clinic-stats team-summary">{[['Total de membros', members.length], ['Veterinários', members.filter(m => m.role === 'VETERINARIO' && m.confirmed).length], ['Contas ativas', members.filter(m => m.active && m.confirmed).length], ['Convites pendentes', members.filter(m => !m.confirmed).length]].map(([label, value]) => <div className="clinic-stat" key={label}><span><Users size={20} /></span><strong>{resource.loading ? '—' : value}</strong><small>{label}</small></div>)}</div>
    <div className="clinic-toolbar clinic-panel"><label className="clinic-search"><span>Buscar membro</span><span className="clinic-search-input"><Search size={18} /><input type="search" placeholder="Nome ou e-mail" value={search} onChange={e => { setSearch(e.target.value); setPage(0) }} /></span></label><div className="clinic-filters" role="group" aria-label="Filtrar equipe">{filters.map(([value, label]) => <button key={value} aria-pressed={filter === value} className={filter === value ? 'selected' : ''} onClick={() => { setFilter(value); setPage(0) }}>{label}</button>)}</div><button onClick={resource.reload} disabled={resource.loading}>Atualizar</button></div>
    <ResourceState {...resource} />
    {resource.data && <><section className="clinic-table-panel"><div className="clinic-table-scroll"><table className="clinic-table"><thead><tr><th>Membro</th><th>Permissão</th><th>Status da conta</th><th>Adicionado em</th><th>Ações</th></tr></thead><tbody>{result.items.map(member => <tr key={member.id}>
      <td><div className="clinic-identity"><span className="clinic-avatar">{!member.confirmed ? <Mail size={17} /> : initials(member.name)}</span><span><strong>{member.name}</strong><small>{member.email}</small></span></div></td>
      <td><span className={'clinic-badge ' + (member.role === 'ADMIN' ? 'status-ativa' : '')}>{roleLabels[member.role]}</span></td>
      <td><span className={'clinic-badge ' + (!member.confirmed ? 'status-manutencao' : member.active ? 'status-disponivel' : '')}>{!member.confirmed ? 'Convite pendente' : member.active ? 'Ativa' : 'Inativa'}</span>{member.confirmed && <small className="clinic-cell-detail">{member.lastActivityAt ? 'Último acesso: ' + dateTime(member.lastActivityAt) : 'Ainda não acessou'}</small>}</td>
      <td>{new Date(member.createdAt).toLocaleDateString('pt-BR')}</td><td><div className="clinic-table-actions">{!member.confirmed ? <button aria-label={'Reenviar convite para ' + member.name} onClick={() => setResending(member)}><Send size={15} />Reenviar</button> : <Link className="clinic-button" to={'/sessoes?usuario=' + member.id}><ShieldCheck size={15} />Sessões</Link>}</div></td>
    </tr>)}</tbody></table></div>{!result.total && <EmptyState><p>Tente outro nome ou permissão.</p></EmptyState>}</section><Pagination {...result} onChange={setPage} /></>}
    {invite && <Editor title="Convidar membro" description="Enviaremos um convite para escolher a senha e ativar a conta." submitLabel="Enviar convite" fields={[{ name: 'name', label: 'Nome completo', maxLength: 120, wide: true }, { name: 'email', label: 'E-mail', type: 'email', maxLength: 254, wide: true }, { name: 'role', label: 'Permissão', value: 'VETERINARIO', options: Object.entries(roleLabels).map(([value, label]) => ({ value, label })), wide: true }]} onCancel={() => setInvite(false)} onSave={async data => { const created = await usersApi.create({ name: String(data.name), email: String(data.email), role: data.role as Role }); setSuccess('Convite enviado para ' + created.email + '.'); resource.reload() }} />}
    {resending && <Editor title="Reenviar convite?" description={'Um novo convite será enviado para ' + resending.email + '.'} fields={[]} submitLabel="Reenviar convite" onCancel={() => setResending(null)} onSave={async () => { await usersApi.resendInvitation(resending.id); setSuccess('Novo convite enviado para ' + resending.email + '.'); resource.reload() }} />}
  </Page>
}

