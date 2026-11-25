import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Mail, Search, Send, ShieldCheck, UserPlus, Users } from 'lucide-react'
import { useResource } from '../hooks/useResource'
import { Editor, EmptyState, Notice, Page, Pagination, ResourceState } from '../components/ClinicUI'
import { usersApi, type Role, type User } from '../services/api'
import { clinicApi, dateTime } from '../services/clinic'
import { useAuth } from '../auth/useAuth'

const roleLabels: Record<Role, string> = {
  ADMIN: 'Administrador',
  VETERINARIO: 'Veterinário',
  RECEPCIONISTA: 'Recepcionista',
  AUXILIAR_TECNICO: 'Auxiliar técnico',
  TUTOR: 'Tutor',
}
const filters = [
  ['', 'Todos'],
  ['ADMIN', 'Admins'],
  ['VETERINARIO', 'Veterinários'],
  ['RECEPCIONISTA', 'Recepcionistas'],
  ['AUXILIAR_TECNICO', 'Auxiliares'],
  ['TUTOR', 'Tutores'],
  ['PENDING', 'Pendentes'],
  ['INACTIVE', 'Inativos'],
]
const initials = (name: string) =>
  name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map(part => part[0]?.toUpperCase())
    .join('')

export function Team() {
  const { user } = useAuth()
  const [editing, setEditing] = useState<User | null>(null)
  const [togglingStatus, setTogglingStatus] = useState<User | null>(null)
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('')
  const [page, setPage] = useState(0)
  const [invite, setInvite] = useState(false)
  const [resending, setResending] = useState<User | null>(null)
  const [success, setSuccess] = useState('')
  const resource = useResource(
    () =>
      clinicApi.list<User>('/users', page, {
        name: search.trim() || undefined,
        role: filter && filter !== 'PENDING' && filter !== 'INACTIVE' ? filter : undefined,
        confirmed: filter === 'PENDING' ? false : filter === 'INACTIVE' ? true : undefined,
        active: filter === 'INACTIVE' ? false : undefined,
      }),
    [page, search, filter]
  )
  const result = resource.data
  return (
    <Page
      title="Equipe"
      description="Gerencie quem tem acesso à clínica e acompanhe seus convites."
      action={
        <button className="clinic-primary" onClick={() => setInvite(true)}>
          <UserPlus size={17} />
          Convidar membro
        </button>
      }
    >
      {success && <Notice>{success}</Notice>}
      <div className="clinic-stats team-summary">
        {[
          ['Total de membros', result?.total ?? 0],
        ].map(([label, value]) => (
          <div className="clinic-stat" key={label}>
            <span>
              <Users size={20} />
            </span>
            <strong>{resource.loading ? '—' : value}</strong>
            <small>{label}</small>
          </div>
        ))}
      </div>
      <div className="clinic-toolbar clinic-panel">
        <label className="clinic-search">
          <span>Buscar membro</span>
          <span className="clinic-search-input">
            <Search size={18} />
            <input
              type="search"
              placeholder="Nome ou e-mail"
              value={search}
              onChange={e => {
                setSearch(e.target.value)
                setPage(0)
              }}
            />
          </span>
        </label>
        <div className="clinic-filters" role="group" aria-label="Filtrar equipe">
          {filters.map(([value, label]) => (
            <button
              key={value}
              aria-pressed={filter === value}
              className={filter === value ? 'selected' : ''}
              onClick={() => {
                setFilter(value)
                setPage(0)
              }}
            >
              {label}
            </button>
          ))}
        </div>
        <button onClick={resource.reload} disabled={resource.loading}>
          Atualizar
        </button>
      </div>
      <ResourceState {...resource} />
      {result && (
        <>
          <section className="clinic-table-panel">
            <div className="clinic-table-scroll">
              <table className="clinic-table">
                <thead>
                  <tr>
                    <th>Membro</th>
                    <th>Permissão</th>
                    <th>Status da conta</th>
                    <th>Adicionado em</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {result.items.map(member => (
                    <tr key={member.id}>
                      <td>
                        <div className="clinic-identity">
                          <span className="clinic-avatar">
                            {!member.confirmed ? <Mail size={17} /> : initials(member.name)}
                          </span>
                          <span>
                            <strong>{member.name}</strong>
                            <small>{member.email}</small>
                          </span>
                        </div>
                      </td>
                      <td>
                        <span
                          className={
                            'clinic-badge ' + (member.role === 'ADMIN' ? 'status-ativa' : '')
                          }
                        >
                          {roleLabels[member.role]}
                        </span>
                      </td>
                      <td>
                        <span
                          className={
                            'clinic-badge ' +
                            (!member.confirmed
                              ? 'status-manutencao'
                              : member.active
                                ? 'status-disponivel'
                                : '')
                          }
                        >
                          {!member.confirmed
                            ? 'Convite pendente'
                            : member.active
                              ? 'Ativa'
                              : 'Inativa'}
                        </span>
                        {member.confirmed && (
                          <small className="clinic-cell-detail">
                            {member.lastActivityAt
                              ? 'Último acesso: ' + dateTime(member.lastActivityAt)
                              : 'Ainda não acessou'}
                          </small>
                        )}
                      </td>
                      <td>{new Date(member.createdAt).toLocaleDateString('pt-BR')}</td>
                      <td>
                        <div className="clinic-table-actions">
                          <button
                            disabled={member.id === user?.id}
                            title={member.id === user?.id ? 'Peça a outro administrador para alterar sua permissão.' : undefined}
                            aria-label={'Editar permissão de ' + member.name}
                            onClick={() => setEditing(member)}
                          >
                            Editar permissão
                          </button>
                          {!member.confirmed ? (
                            <button
                              aria-label={'Reenviar convite para ' + member.name}
                              onClick={() => setResending(member)}
                            >
                              <Send size={15} />
                              Reenviar
                            </button>
                          ) : (
                            <>
                              <button
                                disabled={member.id === user?.id}
                                title={
                                  member.id === user?.id
                                    ? 'Peça a outro administrador para alterar o status da sua conta.'
                                    : undefined
                                }
                                aria-label={
                                  (member.active ? 'Desativar conta de ' : 'Ativar conta de ') +
                                  member.name
                                }
                                onClick={() => setTogglingStatus(member)}
                              >
                                {member.active ? 'Desativar' : 'Ativar'}
                              </button>
                              <Link className="clinic-button" to={'/sessoes?usuario=' + member.id}>
                                <ShieldCheck size={15} />
                                Sessões
                              </Link>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {!result.total && (
              <EmptyState>
                <p>Tente outro nome ou permissão.</p>
              </EmptyState>
            )}
          </section>
          <Pagination {...result} onChange={setPage} />
        </>
      )}
      {invite && (
        <Editor
          title="Convidar membro"
          description="Enviaremos um convite para escolher a senha e ativar a conta."
          submitLabel="Enviar convite"
          fields={[
            { name: 'name', label: 'Nome completo', maxLength: 120, wide: true },
            { name: 'email', label: 'E-mail', type: 'email', maxLength: 254, wide: true },
            {
              name: 'role',
              label: 'Permissão',
              value: 'VETERINARIO',
              options: Object.entries(roleLabels).map(([value, label]) => ({ value, label })),
              wide: true,
            },
          ]}
          onCancel={() => setInvite(false)}
          onSave={async data => {
            const created = await usersApi.create({
              name: String(data.name),
              email: String(data.email),
              role: data.role as Role,
            })
            setSuccess('Convite enviado para ' + created.email + '.')
            resource.reload()
          }}
        />
      )}
      {editing && <Editor
        title="Editar permissão"
        description={<><p>{editing.name} · {editing.email}</p><p>A alteração será aplicada no próximo acesso às funções do sistema.</p>
          <p>Administrador: gestão completa. Veterinário: atendimento clínico. Auxiliar técnico: cuidados e evoluções. Recepcionista: cadastros e internações. Tutor: sem acesso à área da equipe.</p></>}
        fields={[{ name: 'role', label: 'Permissão', value: editing.role, wide: true,
          options: Object.entries(roleLabels).map(([value, label]) => ({ value, label })) }]}
        submitLabel="Salvar permissão"
        onCancel={() => setEditing(null)}
        onSave={async data => {
          await usersApi.updateRole(editing.id, data.role as Role)
          setSuccess('Permissão de ' + editing.name + ' atualizada.')
          resource.reload()
        }}
      />}
      {resending && (
        <Editor
          title="Reenviar convite?"
          description={'Um novo convite será enviado para ' + resending.email + '.'}
          fields={[]}
          submitLabel="Reenviar convite"
          onCancel={() => setResending(null)}
          onSave={async () => {
            await usersApi.resendInvitation(resending.id)
            setSuccess('Novo convite enviado para ' + resending.email + '.')
            resource.reload()
          }}
        />
      )}
      {togglingStatus && (
        <Editor
          title={(togglingStatus.active ? 'Desativar' : 'Ativar') + ' conta?'}
          description={
            togglingStatus.active
              ? 'Ao desativar ' +
                togglingStatus.name +
                ', todas as sessões ativas serão encerradas e o acesso ficará bloqueado até a conta ser reativada.'
              : 'Ao ativar ' +
                togglingStatus.name +
                ', o membro voltará a acessar o sistema com a permissão ' +
                roleLabels[togglingStatus.role] +
                '.'
          }
          fields={[]}
          submitLabel={togglingStatus.active ? 'Desativar conta' : 'Ativar conta'}
          onCancel={() => setTogglingStatus(null)}
          onSave={async () => {
            const nextActive = !togglingStatus.active
            await usersApi.updateStatus(togglingStatus.id, nextActive)
            setSuccess(
              'Conta de ' +
                togglingStatus.name +
                (nextActive ? ' ativada.' : ' desativada.')
            )
            resource.reload()
          }}
        />
      )}
    </Page>
  )
}
