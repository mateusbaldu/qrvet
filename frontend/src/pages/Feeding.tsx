import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { CalendarDays, Check, Clock, Plus, RefreshCw, Search, Utensils } from 'lucide-react'
import { Editor, EmptyState, Notice, Page, ResourceState, type Field } from '../components/ClinicUI'
import { useResource } from '../hooks/useResource'
import { ApiError, request } from '../services/api'
import {
  agendaPath,
  allPages,
  clinicApi,
  dateTime,
  localDate,
  searchText,
  type AgendaAlimentacao,
  type Cuidado,
} from '../services/clinic'

const timeLabel = (value: string) =>
  new Date(value).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })

function ScheduleEditor({
  admissions,
  admissionId,
  onClose,
  onSaved,
}: {
  admissions: Cuidado[]
  admissionId?: string
  onClose: () => void
  onSaved: () => void
}) {
  const [start, setStart] = useState(localDate())
  const [end, setEnd] = useState(localDate())
  const [times, setTimes] = useState(['08:00', '14:00', '20:00'])
  const fields: Field[] = [
    ...(!admissionId
      ? [
          {
            name: 'internacaoId',
            label: 'Paciente internado',
            type: 'number',
            wide: true,
            options: admissions.map(i => ({
              value: i.id,
              label: i.pacienteNome + ' · Baia ' + i.baiaIdentificacao,
            })),
          },
        ]
      : []),
    {
      name: 'alimento',
      label: 'Alimento',
      maxLength: 100,
      hint: 'Conforme orientação da equipe clínica.',
    },
    {
      name: 'quantidade',
      label: 'Quantidade (inclua a unidade)',
      maxLength: 50,
      hint: 'Ex.: 40 g, 50 ml',
    },
  ]
  return (
    <Editor
      title="Agendar alimentação"
      description={
        (admissionId
          ? (admissions.find(i => String(i.id) === admissionId)?.pacienteNome ??
              'Internação #' + admissionId) + ' · '
          : '') +
        'Defina os dias e os horários. Será criado um item no checklist para cada refeição.'
      }
      fields={fields}
      submitLabel="Criar agendamentos"
      onCancel={onClose}
      afterFields={
        <section className="clinic-form-section">
          <div className="clinic-fields">
            <label>
              Data inicial *
              <input
                name="inicio"
                type="date"
                value={start}
                min={localDate()}
                required
                onChange={e => {
                  setStart(e.target.value)
                  if (e.target.value > end) setEnd(e.target.value)
                }}
              />
            </label>
            <label>
              Repetir diariamente até *
              <input
                name="fim"
                type="date"
                value={end}
                min={start}
                required
                onChange={e => setEnd(e.target.value)}
              />
              <small>Use a mesma data para agendar apenas um dia. Até 31 dias.</small>
            </label>
          </div>
          <h3>Horários de cada dia</h3>
          <div className="clinic-time-inputs">
            {times.map((time, index) => (
              <label key={index}>
                Horário {index + 1}
                <span>
                  <input
                    type="time"
                    aria-label={'Horário ' + (index + 1)}
                    value={time}
                    required
                    onChange={e =>
                      setTimes(times.map((t, i) => (i === index ? e.target.value : t)))
                    }
                  />
                  <button
                    type="button"
                    disabled={times.length === 1}
                    aria-label={'Remover horário ' + (index + 1)}
                    onClick={() => setTimes(times.filter((_, i) => i !== index))}
                  >
                    ×
                  </button>
                </span>
              </label>
            ))}
          </div>
          <button
            type="button"
            disabled={times.length >= 8}
            onClick={() => setTimes([...times, ''])}
          >
            <Plus size={16} />
            Adicionar horário
          </button>
          <p className="clinic-muted">
            Horários locais deste dispositivo. Horários de hoje que já passaram serão ignorados. Um
            jejum ativo impede a conclusão da refeição.
          </p>
        </section>
      }
      onSave={async data => {
        const dates: string[] = []
        const first = new Date(start + 'T00:00:00'),
          last = new Date(end + 'T00:00:00')
        if (new Set(times).size !== times.length)
          throw new ApiError(400, 'Remova os horários repetidos.')
        if (last < first || (Date.parse(end) - Date.parse(start)) / 86400000 > 30)
          throw new ApiError(400, 'Selecione um período de até 31 dias.')
        for (const day = new Date(first); day <= last; day.setDate(day.getDate() + 1))
          for (const time of times) {
            const at = new Date(localDate(day) + 'T' + time + ':00')
            if (at.getTime() > Date.now()) dates.push(at.toISOString())
          }
        if (!dates.length) throw new ApiError(400, 'Selecione pelo menos um horário futuro.')
        await clinicApi.save(
          '/internacoes/' + (admissionId ?? data.internacaoId) + '/alimentacao/agenda',
          { alimento: data.alimento, quantidade: data.quantidade, horarios: dates }
        )
        onSaved()
      }}
    />
  )
}

export function FeedingChecklist({
  admissionId,
  active = true,
  onChanged,
}: {
  admissionId?: string
  active?: boolean
  onChanged?: () => void
}) {
  const [date, setDate] = useState(localDate())
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('TODAS')
  const [creating, setCreating] = useState(false)
  const [schedulePatient, setSchedulePatient] = useState<string>()
  const [action, setAction] = useState<{
    item: AgendaAlimentacao
    kind: 'concluir' | 'cancelar'
  } | null>(null)
  const [success, setSuccess] = useState('')
  const [now, setNow] = useState(Date.now)
  const resource = useResource(
    () => request<AgendaAlimentacao[]>(agendaPath(date, admissionId)),
    date + ':' + (admissionId ?? '') + ':' + active
  )
  const directory = useResource(
    () =>
      admissionId ? Promise.resolve([] as Cuidado[]) : allPages<Cuidado>('/internacoes/cuidados'),
    admissionId ?? 'directory'
  )
  const reload = resource.reload
  useEffect(() => {
    const refresh = () => {
      setNow(Date.now())
      reload()
    }
    const timer = window.setInterval(() => {
      if (!document.hidden) refresh()
    }, 60000)
    window.addEventListener('focus', refresh)
    return () => {
      clearInterval(timer)
      window.removeEventListener('focus', refresh)
    }
  }, [date, admissionId, active, reload])
  const state = (item: AgendaAlimentacao) =>
    item.status !== 'PENDENTE'
      ? item.status
      : item.jejumAtivo
        ? 'JEJUM'
        : Date.parse(item.horario) <= now
          ? 'ATRASADA'
          : 'PENDENTE'
  const items = resource.data ?? []
  const pending = items.filter(i => i.status === 'PENDENTE')
  const done = items.filter(i => i.status === 'CONCLUIDA').length
  const unscheduled = (directory.data ?? []).filter(
    i => !items.some(a => a.internacaoId === i.id && a.status !== 'CANCELADA')
  )
  const filtered = items.filter(
    i =>
      (filter === 'TODAS' ||
        (filter === 'PENDENTE' ? i.status === 'PENDENTE' : state(i) === filter)) &&
      searchText(i.pacienteNome + ' ' + i.baiaIdentificacao).includes(searchText(search.trim()))
  )
  const labels: Record<string, string> = {
    PENDENTE: 'Programada',
    CONCLUIDA: 'Concluída',
    CANCELADA: 'Cancelada',
    JEJUM: 'Em jejum',
    ATRASADA: 'Pendente desde ',
  }
  const refresh = () => {
    resource.reload()
    directory.reload()
    setNow(Date.now())
  }
  return (
    <section className="clinic-panel clinic-feeding">
      <div className="clinic-section-heading">
        <div>
          <h2>
            <Utensils size={20} />
            {admissionId ? 'Agenda de alimentação' : 'Checklist de alimentação'}
          </h2>
          <p>
            {admissionId
              ? 'Horários e refeições desta internação.'
              : 'Todos os pacientes, organizados pelo horário da refeição.'}
          </p>
        </div>
        <div className="clinic-actions">
          <button aria-label="Atualizar checklist" onClick={refresh}>
            <RefreshCw size={16} />
          </button>
          {active && (
            <button
              className="clinic-primary"
              onClick={() => setCreating(true)}
              disabled={!admissionId && !directory.data?.length}
            >
              <Plus size={16} />
              Agendar alimentação
            </button>
          )}
        </div>
      </div>
      {success && <Notice>{success}</Notice>}
      <div className="clinic-agenda-summary" aria-label="Resumo do dia">
        <span>
          <strong>{pending.length}</strong> pendentes
        </span>
        <span className="agenda-late">
          <strong>
            {pending.filter(i => !i.jejumAtivo && Date.parse(i.horario) <= now).length}
          </strong>{' '}
          atrasadas
        </span>
        <span>
          <strong>{done}</strong> concluídas
        </span>
        <span>
          <strong>{pending.filter(i => i.jejumAtivo).length}</strong> em jejum
        </span>
      </div>
      <div className="clinic-toolbar">
        <label>
          Dia do checklist
          <input
            aria-label="Dia do checklist"
            type="date"
            value={date}
            required
            onChange={e => {
              if (e.target.value) setDate(e.target.value)
            }}
          />
        </label>
        <button onClick={() => setDate(localDate())}>Hoje</button>
        {!admissionId && (
          <label className="clinic-search">
            Buscar paciente ou baia
            <span className="clinic-search-input">
              <Search size={17} />
              <input
                type="search"
                value={search}
                onChange={e => setSearch(e.target.value)}
                placeholder="Nome do paciente ou baia"
              />
            </span>
          </label>
        )}
      </div>
      <div className="clinic-filters" role="group" aria-label="Filtrar refeições">
        {[
          ['TODAS', 'Todas'],
          ['PENDENTE', 'Pendentes'],
          ['ATRASADA', 'Atrasadas'],
          ['JEJUM', 'Em jejum'],
          ['CONCLUIDA', 'Concluídas'],
          ['CANCELADA', 'Canceladas'],
        ].map(([value, label]) => (
          <button
            key={value}
            aria-pressed={filter === value}
            className={filter === value ? 'selected' : ''}
            onClick={() => setFilter(value)}
          >
            {label}
          </button>
        ))}
      </div>
      <ResourceState {...resource} />
      {!admissionId && <ResourceState {...directory} />}
      {resource.data && (
        <>
          {filtered.length ? (
            <ul className="clinic-feeding-list">
              {filtered.map(item => {
                const status = state(item),
                  future = Date.parse(item.horario) > now
                return (
                  <li key={item.id} className={'feeding-' + status.toLowerCase()}>
                    <div className="feeding-time">
                      <Clock size={16} />
                      <time dateTime={item.horario}>{timeLabel(item.horario)}</time>
                    </div>
                    <div className="feeding-patient">
                      <strong>{item.pacienteNome}</strong>
                      <small>
                        Baia {item.baiaIdentificacao} · Internação #{item.internacaoId}
                      </small>
                      <p>
                        {item.alimento} · {item.quantidade}
                      </p>
                      {item.status === 'CONCLUIDA' && (
                        <small>
                          Concluída {dateTime(item.concluidaEm)} · {item.responsavelNome}
                          {item.observacao && ' · ' + item.observacao}
                        </small>
                      )}
                      {item.status === 'CANCELADA' && <small>{item.motivoCancelamento}</small>}
                    </div>
                    <div className="feeding-result">
                      <span className={'clinic-badge status-' + status.toLowerCase()}>
                        {labels[status]}
                        {status === 'ATRASADA' && timeLabel(item.horario)}
                      </span>
                      {item.status === 'PENDENTE' && (
                        <>
                          <button
                            className="feeding-check"
                            disabled={
                              item.jejumAtivo || future || item.internacaoStatus !== 'ATIVA'
                            }
                            title={
                              item.jejumAtivo
                                ? 'Encerre o jejum nos cuidados antes de alimentar.'
                                : future
                                  ? 'Disponível a partir do horário agendado.'
                                  : 'Confirmar refeição e registrar no histórico'
                            }
                            onClick={() => setAction({ item, kind: 'concluir' })}
                          >
                            <Check size={17} />
                            Concluir
                          </button>
                          <button
                            className="feeding-cancel"
                            aria-label={
                              'Cancelar alimentação de ' +
                              item.pacienteNome +
                              ' às ' +
                              timeLabel(item.horario)
                            }
                            onClick={() => setAction({ item, kind: 'cancelar' })}
                          >
                            Cancelar
                          </button>
                        </>
                      )}
                      {!admissionId && (
                        <Link to={'/cuidados?internacao=' + item.internacaoId}>Ver cuidados →</Link>
                      )}
                    </div>
                  </li>
                )
              })}
            </ul>
          ) : (
            <EmptyState
              title={
                items.length
                  ? 'Nenhuma refeição neste filtro'
                  : 'Nenhuma alimentação agendada para este dia'
              }
            >
              <p>
                {items.length
                  ? 'Altere os filtros para ver outras refeições.'
                  : 'Escolha outro dia ou agende os horários de alimentação dos pacientes.'}
              </p>
            </EmptyState>
          )}
        </>
      )}
      <p className="clinic-muted">
        Horários locais deste dispositivo · atualização automática a cada minuto. A conclusão
        registra a alimentação no histórico com data, hora e responsável.
      </p>
      {!admissionId && resource.data && unscheduled.length > 0 && (
        <details className="clinic-unscheduled">
          <summary>{unscheduled.length} paciente(s) sem refeições programadas neste dia</summary>
          <p className="clinic-muted">
            Confira quais pacientes precisam de horários, conforme a orientação da equipe.
          </p>
          {unscheduled.map(i => (
            <div className="clinic-section-heading" key={i.id}>
              <span>
                {i.pacienteNome} · Baia {i.baiaIdentificacao}
              </span>
              <button
                onClick={() => {
                  setSchedulePatient(String(i.id))
                  setCreating(true)
                }}
              >
                Agendar para {i.pacienteNome}
              </button>
            </div>
          ))}
        </details>
      )}
      {creating && (
        <ScheduleEditor
          admissions={directory.data ?? []}
          admissionId={admissionId ?? schedulePatient}
          onClose={() => {
            setCreating(false)
            setSchedulePatient(undefined)
          }}
          onSaved={() => {
            setSuccess('Alimentação agendada. Consulte os dias escolhidos no checklist.')
            refresh()
          }}
        />
      )}
      {action && (
        <Editor
          title={action.kind === 'concluir' ? 'Concluir alimentação' : 'Cancelar alimentação'}
          description={
            <>
              {action.item.pacienteNome} · {dateTime(action.item.horario)}
              <br />
              {action.item.alimento} · {action.item.quantidade}
              {action.kind === 'concluir' && (
                <p>
                  Confirme somente após oferecer a refeição. Registre abaixo a aceitação ou eventual
                  recusa.
                </p>
              )}
            </>
          }
          submitLabel={
            action.kind === 'concluir' ? 'Confirmar alimentação' : 'Confirmar cancelamento'
          }
          fields={
            action.kind === 'concluir'
              ? [
                  {
                    name: 'observacao',
                    label: 'Aceitação e observações',
                    type: 'textarea',
                    required: false,
                  },
                ]
              : [{ name: 'motivo', label: 'Motivo do cancelamento', maxLength: 255, wide: true }]
          }
          onCancel={() => setAction(null)}
          onSave={async values => {
            await clinicApi.save(
              '/internacoes/' +
                action.item.internacaoId +
                '/alimentacao/agenda/' +
                action.item.id +
                '/' +
                action.kind,
              values,
              'PUT'
            )
            setSuccess(
              action.kind === 'concluir'
                ? 'Alimentação concluída e registrada no histórico.'
                : 'Agendamento cancelado.'
            )
            refresh()
            onChanged?.()
          }}
        />
      )}
    </section>
  )
}

export function Feeding() {
  const [params] = useSearchParams()
  const id = params.get('internacao') ?? undefined
  return (
    <Page
      title="Alimentação"
      description="Organize os horários e acompanhe as refeições da equipe em um só lugar."
      action={
        <Link className="clinic-button" to="/cuidados">
          <CalendarDays size={17} />
          Cuidados por paciente
        </Link>
      }
    >
      <FeedingChecklist key={id} admissionId={id} />
    </Page>
  )
}
