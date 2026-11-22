import { useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  Download,
  HeartPulse,
  NotebookPen,
  PawPrint,
  RefreshCw,
  ShieldCheck,
  ShieldAlert,
} from 'lucide-react'
import { useResource } from '../hooks/useResource'
import {
  Editor,
  EmptyState,
  Notice,
  Page,
  Pagination,
  ResourceState,
  StatusBadge,
} from '../components/ClinicUI'
import { readableError, request, type User } from '../services/api'
import { allPages, clinicApi, dateTime, type AuditLog, type Session } from '../services/clinic'
import '../styles/clinical-records.css'

export function PublicHospitalization() {
  const { token = '' } = useParams()
  const [evolutionPage, setEvolutionPage] = useState(0)
  const resource = useResource(async () => ({ ...await clinicApi.publicDetails(token, evolutionPage), consultadoEm: new Date().toISOString() }), [token, evolutionPage])
  const i = resource.data
  const alertFields = [
    ['alergias', 'Alergias'],
    ['restricoesAlimentares', 'Restrições alimentares'],
    ['cuidadosManejo', 'Cuidados de manejo'],
  ] as const
  const activeAlerts = alertFields.filter(([key]) => i?.alertas?.[key]?.trim())
  return (
    <main className="clinic-public">
      <header className="public-brand">
        <span className="sidebar-logo">
          <PawPrint size={24} />
        </span>
        <div>
          <strong>QRVet</strong>
          <small>Perto de quem você ama.</small>
        </div>
        <span className="public-readonly">
          <ShieldCheck size={15} />
          Acompanhamento do tutor
        </span>
      </header>
      <div className="clinic-page public-content">
        <p className="clinic-eyebrow">ACOMPANHAMENTO DA INTERNAÇÃO</p>
        <div className="public-refresh">
          <p>{i ? <>Consultado em <time dateTime={i.consultadoEm}>{dateTime(i.consultadoEm)}</time></> : 'Informações registradas pela equipe da clínica.'}</p>
          <button className="clinic-button" disabled={resource.loading} onClick={resource.reload}>
            <RefreshCw size={16} aria-hidden="true" />{resource.loading ? 'Atualizando...' : 'Atualizar informações'}
          </button>
        </div>
        <ResourceState {...resource} />
        {i && (
          <>
            <section className="public-patient">
              <div className="clinic-section-heading">
                <span className="clinic-avatar large">
                  <PawPrint size={30} />
                </span>
                <StatusBadge status={i.status} />
              </div>
              <h1>{i.pacienteNome}</h1>
              <p>
                {i.especie} · {i.raca} · {i.sexo}
              </p>
              <div className="public-location">
                Baia <strong>{i.baiaIdentificacao}</strong>
                <span>Entrada em {dateTime(i.entradaInternacao)}</span>
              </div>
            </section>
            {i.jejumAtivo && i.status === 'ATIVA' && (
              <p className="clinic-notice" role="status">
                <strong>Paciente em jejum.</strong>
                <br />
                Não oferecer alimento. A equipe está acompanhando os cuidados.
              </p>
            )}
            <section className={'clinic-panel patient-alerts' + (activeAlerts.length ? ' has-alerts' : '')} aria-labelledby="public-alerts-title">
              <h2 id="public-alerts-title"><ShieldAlert size={20} aria-hidden="true" />Alertas e restrições</h2>
              {activeAlerts.length ? <dl className="patient-alerts-list">
                {activeAlerts.map(([key, label]) => <div key={key}><dt>{label}</dt><dd>{i.alertas[key]}</dd></div>)}
              </dl> : <p className="clinic-muted">Nenhum alerta ou restrição registrado. Confirme as orientações com a equipe da clínica.</p>}
            </section>
            <section className="clinic-panel">
              <h2>
                <HeartPulse size={20} />
                Informações da internação
              </h2>
              <dl className="clinic-details single">
                <div>
                  <dt>Motivo da internação</dt>
                  <dd>{i.motivo}</dd>
                </div>
                <div>
                  <dt>Veterinário responsável</dt>
                  <dd>{i.veterinarioNome || 'Não informado'}</dd>
                </div>
                {i.saidaInternacao && <div>
                  <dt>Encerramento da internação</dt>
                  <dd>{dateTime(i.saidaInternacao)}</dd>
                </div>}
                <div>
                  <dt>Situação</dt>
                  <dd>
                    {i.status === 'ATIVA'
                      ? 'Paciente sob os cuidados da equipe veterinária.'
                      : i.status === 'ALTA'
                        ? 'Internação encerrada com alta.'
                        : 'Internação encerrada por óbito.'}
                  </dd>
                </div>
              </dl>
            </section>
            <section className="clinic-panel evolution-notes" aria-labelledby="public-evolutions-title">
              <h2 id="public-evolutions-title"><NotebookPen size={20} aria-hidden="true" />Evoluções do paciente</h2>
              <p className="clinic-muted">Acompanhe as anotações da equipe, da mais recente à mais antiga.</p>
              {i.evolucoes?.items.length ? <>
                <p className="clinic-muted">{i.evolucoes.total} {i.evolucoes.total === 1 ? 'evolução registrada' : 'evoluções registradas'}</p>
                <ol className="evolution-notes-list">{i.evolucoes.items.map((note, index) => <li key={index}>
                  <header><strong>{note.profissionalNome}</strong><time dateTime={note.dataHoraRegistro}>{dateTime(note.dataHoraRegistro)}</time></header>
                  <p>{note.texto}</p>
                </li>)}</ol>
                {i.evolucoes.pages > 1 && <Pagination {...i.evolucoes} onChange={setEvolutionPage} />}
              </> : <EmptyState title="Nenhuma evolução registrada"><p>As atualizações desta internação aparecerão aqui assim que forem registradas pela equipe.</p></EmptyState>}
            </section>
            <p className="public-note">
              As informações refletem os registros da equipe. Para esclarecer dúvidas sobre os cuidados e a evolução do paciente, entre em contato com a clínica.
            </p>
          </>
        )}
        <footer className="public-footer">
          <span>
            <ShieldCheck size={15} />
            Cuidado com responsabilidade
          </span>
        </footer>
      </div>
    </main>
  )
}

export function Sessions() {
  const [params] = useSearchParams()
  const [userId, setUserId] = useState(params.get('usuario') ?? '')
  const [page, setPage] = useState(0)
  const [revoke, setRevoke] = useState<Session | 'all' | null>(null)
  const [success, setSuccess] = useState('')
  const users = useResource(() => allPages<User>('/users'), ['users'])
  const resource = useResource(
    () =>
      userId
        ? clinicApi.list<Session>('/users/' + userId + '/sessions', page)
        : Promise.resolve({ items: [], page: 0, size: 12, total: 0, pages: 0 }),
    [userId, page]
  )
  return (
    <Page
      title="Sessões da equipe"
      description="Acompanhe os acessos e encerre sessões quando necessário."
      action={
        <Link to="/equipe" className="clinic-button">
          Voltar à equipe
        </Link>
      }
    >
      {success && <Notice>{success}</Notice>}
      <ResourceState {...users} />
      <div className="clinic-panel">
        <label>
          Membro da equipe
          <select
            value={userId}
            onChange={e => {
              setUserId(e.target.value)
              setPage(0)
              setRevoke(null)
              setSuccess('')
            }}
          >
            <option value="">Selecione um membro</option>
            {users.data?.map(u => (
              <option value={u.id} key={u.id}>
                {u.name} · {u.email}
              </option>
            ))}
          </select>
        </label>
      </div>
      {userId ? (
        <>
          <ResourceState {...resource} />
          {revoke && (
            <Editor
              title={
                revoke === 'all'
                  ? 'Encerrar todas as sessões deste usuário?'
                  : 'Encerrar esta sessão?'
              }
              description="O membro precisará entrar novamente. Confira o usuário selecionado antes de confirmar."
              fields={[]}
              submitLabel="Confirmar encerramento"
              onCancel={() => setRevoke(null)}
              onSave={async () => {
                await request(
                  '/users/' +
                    userId +
                    '/sessions' +
                    (revoke === 'all' ? '' : '/' + encodeURIComponent(revoke.jti)),
                  { method: 'DELETE' }
                )
                setSuccess('Acesso encerrado com sucesso.')
                setPage(0)
                resource.reload()
              }}
            />
          )}
          {resource.data && (
            <>
              <div className="clinic-actions">
                <button disabled={!resource.data.total} onClick={() => setRevoke('all')}>
                  Encerrar todas as sessões
                </button>
              </div>
              <div className="clinic-grid">
                {resource.data.items.map(s => (
                  <article className="clinic-card" key={s.jti}>
                    <span className="clinic-avatar">
                      <ShieldCheck size={23} />
                    </span>
                    <h2>Sessão ativa</h2>
                    <dl className="clinic-details single">
                      <div>
                        <dt>Início</dt>
                        <dd>{dateTime(s.createdAt)}</dd>
                      </div>
                      <div>
                        <dt>Expiração</dt>
                        <dd>{dateTime(s.expiresAt)}</dd>
                      </div>
                    </dl>
                    <button onClick={() => setRevoke(s)}>Encerrar sessão</button>
                  </article>
                ))}
              </div>
              {!resource.data.total && (
                <EmptyState title="Nenhuma sessão ativa">
                  <p>Este membro não possui acessos ativos no momento.</p>
                </EmptyState>
              )}
              <Pagination {...resource.data} onChange={setPage} />
            </>
          )}
        </>
      ) : (
        <EmptyState title="Selecione um membro">
          <p>Os acessos ativos serão exibidos aqui.</p>
        </EmptyState>
      )}
    </Page>
  )
}

function toStartOfDayIso(dateValue: string): string | undefined {
  if (!dateValue) {
    return undefined
  }
  return new Date(dateValue + 'T00:00:00').toISOString()
}

function toEndOfDayIso(dateValue: string): string | undefined {
  if (!dateValue) {
    return undefined
  }
  return new Date(dateValue + 'T23:59:59.999').toISOString()
}

function formatResourceCell(item: AuditLog): string {
  if (!item.resourceId) {
    return item.resourceName
  }
  return item.resourceName + ' #' + item.resourceId
}

export function AuditLogs() {
  const [page, setPage] = useState(0)
  const [userId, setUserId] = useState('')
  const [action, setAction] = useState('')
  const [resourceName, setResourceName] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [downloading, setDownloading] = useState(false)
  const [downloadError, setDownloadError] = useState('')

  const startIso = toStartOfDayIso(startDate)
  const endIso = toEndOfDayIso(endDate)

  const users = useResource(() => allPages<User>('/users'), ['audit-users'])
  const logs = useResource(
    () =>
      clinicApi.list<AuditLog>('/audit-logs', page, {
        userId: userId || undefined,
        action: action || undefined,
        resourceName: resourceName || undefined,
        startDate: startIso,
        endDate: endIso,
      }),
    [page, userId, action, resourceName, startIso, endIso]
  )

  async function downloadCsv() {
    if (downloading) return
    setDownloading(true)
    setDownloadError('')
    try {
      const query = new URLSearchParams()
      if (userId) query.set('userId', userId)
      if (action) query.set('action', action)
      if (resourceName) query.set('resourceName', resourceName)
      if (startIso) query.set('startDate', startIso)
      if (endIso) query.set('endDate', endIso)
      const qs = query.toString()
      let exportPath = '/audit-logs/export'
      if (qs) {
        exportPath = '/audit-logs/export?' + qs
      }
      const blob = await request<Blob>(exportPath, {
        responseType: 'blob',
      })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'relatorio-auditoria.csv'
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    } catch (error) {
      setDownloadError(readableError(error))
    } finally {
      setDownloading(false)
    }
  }

  let downloadButtonLabel = 'Baixar relatório (CSV)'
  if (downloading) {
    downloadButtonLabel = 'Gerando relatório...'
  }

  return (
    <Page
      title="Auditoria"
      help="Filtre os eventos por período, membro da equipe, ação ou recurso e exporte o relatório consolidado em CSV."
      description="Rastreabilidade de acessos e operações críticas realizadas no sistema."
      action={
        <button className="clinic-primary" disabled={downloading} onClick={downloadCsv}>
          <Download size={17} />
          {downloadButtonLabel}
        </button>
      }
    >
      {downloadError && (
        <p className="clinic-error" role="alert">
          {downloadError}
        </p>
      )}
      <div className="clinic-panel">
        <div className="clinic-fields">
          <label>
            Data inicial
            <input
              type="date"
              value={startDate}
              onChange={e => {
                setStartDate(e.target.value)
                setPage(0)
              }}
            />
          </label>
          <label>
            Data final
            <input
              type="date"
              value={endDate}
              onChange={e => {
                setEndDate(e.target.value)
                setPage(0)
              }}
            />
          </label>
          <label>
            Membro da equipe
            <select
              value={userId}
              onChange={e => {
                setUserId(e.target.value)
                setPage(0)
              }}
            >
              <option value="">Todos os usuários</option>
              {users.data?.map(u => (
                <option value={u.id} key={u.id}>
                  {u.name} · {u.email}
                </option>
              ))}
            </select>
          </label>
          <label>
            Recurso
            <select
              value={resourceName}
              onChange={e => {
                setResourceName(e.target.value)
                setPage(0)
              }}
            >
              <option value="">Todos os recursos</option>
              <option value="auth">Autenticação</option>
              <option value="patient">Paciente</option>
              <option value="hospitalization">Internação</option>
            </select>
          </label>
          <label className="clinic-field-wide">
            Ação
            <select
              value={action}
              onChange={e => {
                setAction(e.target.value)
                setPage(0)
              }}
            >
              <option value="">Todas as ações</option>
              <option value="LOGIN_SUCCESS">Login realizado</option>
              <option value="LOGIN_FAILED">Falha no login</option>
              <option value="LOGOUT">Logout realizado</option>
              <option value="PASSWORD_CHANGED">Senha alterada</option>
              <option value="PASSWORD_RECOVERY_REQUESTED">Recuperação de senha solicitada</option>
              <option value="PASSWORD_RESET">Senha redefinida</option>
              <option value="PATIENT_CREATED">Paciente criado</option>
              <option value="PATIENT_UPDATED">Paciente atualizado</option>
              <option value="HOSPITALIZATION_CREATED">Internação criada</option>
              <option value="HOSPITALIZATION_UPDATED">Internação atualizada</option>
            </select>
          </label>
        </div>
      </div>
      <ResourceState {...logs} />
      {logs.data && (
        <>
          <section className="clinic-table-panel">
            <div className="clinic-table-scroll">
              <table className="clinic-table">
                <thead>
                  <tr>
                    <th>Data/Hora</th>
                    <th>Ação</th>
                    <th>Recurso</th>
                    <th>Usuário</th>
                    <th>IP</th>
                    <th>Detalhes</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.data.items.map(item => (
                    <tr key={item.id}>
                      <td>{dateTime(item.timestamp)}</td>
                      <td>
                        <strong>{item.action}</strong>
                      </td>
                      <td>{formatResourceCell(item)}</td>
                      <td>{item.userEmail || '—'}</td>
                      <td>{item.ipAddress || '—'}</td>
                      <td>
                        <small>{item.details || '—'}</small>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {!logs.data.total && (
              <EmptyState title="Nenhum registro de auditoria">
                <p>Altere os filtros para consultar outros eventos.</p>
              </EmptyState>
            )}
          </section>
          <Pagination {...logs.data} onChange={setPage} />
        </>
      )}
    </Page>
  )
}

export function Bootstrap() {
  const navigate = useNavigate()
  const [done, setDone] = useState(false)
  return (
    <main className="clinic-public">
      <Page
        title="Configurar clínica"
        description="Crie a primeira conta de administrador para começar."
      >
        {done ? (
          <section className="clinic-panel">
            <Notice>Administrador criado com sucesso.</Notice>
            <Link to="/login">Entrar</Link>
          </section>
        ) : (
          <Editor
            inline
            closeOnSave={false}
            title="Primeiro administrador"
            fields={[
              { name: 'name', label: 'Nome completo', maxLength: 120 },
              { name: 'email', label: 'E-mail', type: 'email', maxLength: 254 },
              {
                name: 'password',
                label: 'Senha (mínimo de 8 caracteres)',
                type: 'password',
                minLength: 8,
                maxLength: 72,
                autoComplete: 'new-password',
              },
              { name: 'secret', label: 'Chave de configuração', type: 'password' },
            ]}
            onCancel={() => {
              if (!done) navigate('/login')
            }}
            onSave={async data => {
              await request('/bootstrap/admin', {
                method: 'POST',
                headers: { 'X-Bootstrap-Secret': String(data.secret) },
                body: { name: data.name, email: data.email, password: data.password },
                retryAuthentication: false,
                authenticated: false,
              })
              setDone(true)
            }}
          />
        )}
        <Link to="/login">Voltar ao acesso</Link>
      </Page>
    </main>
  )
}
