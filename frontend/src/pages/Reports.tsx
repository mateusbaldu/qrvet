import { useId, useState, type FormEvent, type ReactElement } from 'react'
import { CalendarDays, Clock, Download, Filter, HeartPulse, LogOut, Plus, Stethoscope, type LucideIcon } from 'lucide-react'
import { Page, ResourceState, StatusBadge } from '../components/ClinicUI'
import { useResource } from '../hooks/useResource'
import { readableError, request, type Role } from '../services/api'
import { dateOnly, downloadBlob, type Veterinario } from '../services/clinic'
import { clinicalReportQuery, reportsApi, type ClinicalReport, type ReportFilters } from '../services/reports'
import '../styles/reports.css'

const emptyFilters: ReportFilters = { startDate: '', endDate: '', veterinarioId: '' }
const number = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 })
const roleLabels: Record<Role, string> = {
  ADMIN: 'Administrador',
  VETERINARIO: 'Veterinário',
  RECEPCIONISTA: 'Recepcionista',
  AUXILIAR_TECNICO: 'Auxiliar técnico',
  TUTOR: 'Tutor',
}

function periodLabel({ startDate, endDate }: ReportFilters) {
  if (startDate && endDate) return `${dateOnly(startDate)} a ${dateOnly(endDate)}`
  if (startDate) return `Desde ${dateOnly(startDate)}`
  if (endDate) return `Até ${dateOnly(endDate)}`
  return 'Todo o histórico'
}

export function Reports() {
  const [draft, setDraft] = useState(emptyFilters)
  const [applied, setApplied] = useState(emptyFilters)
  const [dateError, setDateError] = useState('')
  const [downloading, setDownloading] = useState(false)
  const [downloadError, setDownloadError] = useState('')
  const query = clinicalReportQuery(applied)
  const veterinarios = useResource(() => request<Veterinario[]>('/internacoes/veterinarios'), [])
  const resource = useResource(async () => ({ query, report: await reportsApi.clinical(query) }), [query])
  const report = resource.data?.query === query ? resource.data.report : undefined
  const selectedVet = veterinarios.data?.find(vet => String(vet.id) === applied.veterinarioId)
  const hasDraftChanges = draft.startDate !== applied.startDate || draft.endDate !== applied.endDate ||
    draft.veterinarioId !== applied.veterinarioId
  const canClear = Boolean(query || hasDraftChanges)

  function update(field: keyof ReportFilters, value: string) {
    setDraft(previous => ({ ...previous, [field]: value }))
    if (field !== 'veterinarioId') setDateError('')
  }

  function apply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (draft.startDate && draft.endDate && draft.endDate < draft.startDate) {
      setDateError('A data final deve ser igual ou posterior à data inicial.')
      event.currentTarget.querySelector<HTMLInputElement>('[name="endDate"]')?.focus()
      return
    }
    setDateError('')
    setDownloadError('')
    if (hasDraftChanges) setApplied(draft)
    else resource.reload()
  }

  function clear() {
    setDraft(emptyFilters)
    setApplied(emptyFilters)
    setDateError('')
    setDownloadError('')
  }

  async function downloadExcel() {
    if (!report || hasDraftChanges || downloading) return
    setDownloading(true)
    setDownloadError('')
    try {
      const file = await reportsApi.exportClinical(query)
      downloadBlob(file, 'relatorio-performance-clinica.xlsx')
    } catch (error) {
      setDownloadError(readableError(error))
    } finally {
      setDownloading(false)
    }
  }

  return (
    <Page
      title="Relatórios"
      description="Acompanhe as internações, os cuidados da equipe e o uso das baias da clínica."
      action={
        <button type="button" onClick={downloadExcel} disabled={!report || hasDraftChanges || downloading}>
          <Download size={17} aria-hidden="true" />
          {downloading ? 'Gerando planilha…' : 'Baixar Excel'}
        </button>
      }
    >
      <form className="clinic-panel report-filter-panel" onSubmit={apply} aria-labelledby="report-filters-title">
        <h2 id="report-filters-title" className="report-section-title"><Filter size={18} aria-hidden="true" /> Filtros</h2>
        <div className="clinic-fields report-fields">
          <label>
            Data inicial
            <input type="date" name="startDate" max="9999-12-31" value={draft.startDate} onChange={event => update('startDate', event.target.value)} />
          </label>
          <label>
            Data final
            <input
              type="date"
              name="endDate"
              max="9999-12-31"
              value={draft.endDate}
              onChange={event => update('endDate', event.target.value)}
              aria-invalid={Boolean(dateError)}
              aria-describedby={dateError ? 'report-date-error' : undefined}
            />
          </label>
          <label>
            Veterinário responsável
            <select value={draft.veterinarioId} disabled={!veterinarios.data} onChange={event => update('veterinarioId', event.target.value)}>
              <option value="">{veterinarios.loading ? 'Carregando veterinários…' : 'Todos os veterinários'}</option>
              {veterinarios.data?.map(vet => <option key={vet.id} value={vet.id}>{vet.name}</option>)}
            </select>
          </label>
          <div className="report-filter-actions">
            <button type="submit" className="clinic-primary">Aplicar filtros</button>
            <button type="button" onClick={clear} disabled={!canClear}>Limpar</button>
          </div>
        </div>
        {dateError && <p id="report-date-error" className="clinic-field-error report-date-error" role="alert">{dateError}</p>}
        {veterinarios.error && (
          <ResourceState loading={false} error={'Falha ao carregar os veterinários. ' + veterinarios.error} reload={veterinarios.reload} />
        )}
        {hasDraftChanges && !dateError && <p className="report-filter-hint">Aplique os filtros para atualizar os dados e baixar a planilha.</p>}
      </form>

      {downloadError && <p className="clinic-error" role="alert">Falha ao baixar o relatório. {downloadError}</p>}
      <ResourceState
        loading={resource.loading || Boolean(resource.data && !report)}
        error={resource.error}
        reload={resource.reload}
      />
      {report && (
        <>
          <p className="report-period" role="status">
            <CalendarDays size={16} aria-hidden="true" />
            <span><strong>{periodLabel(applied)}</strong> · {selectedVet?.name ?? 'Todos os veterinários'}</span>
          </p>
          <ReportSummary report={report} />
          <ReportDetails report={report} />
        </>
      )}
    </Page>
  )
}

function ReportSummary({ report }: { report: ClinicalReport }) {
  const admissions = report.resumoInternacoes
  const care = report.resumoCuidados
  const stats: { label: string; value: number; icon: LucideIcon; unit?: string }[] = [
    { label: 'Abertas no período', value: admissions.totalNoPeriodo, icon: Plus },
    { label: 'Ainda ativas', value: admissions.ativas, icon: HeartPulse },
    { label: 'Com alta', value: admissions.altas, icon: LogOut },
    { label: 'Com óbito', value: admissions.obitos, icon: HeartPulse },
    { label: 'Tempo médio de internação', value: admissions.tempoMedioInternacaoHoras, icon: Clock, unit: 'h' },
    { label: 'Encerradas no período', value: admissions.encerradasNoPeriodo, icon: CalendarDays },
  ]
  const careStats = [
    ['Evoluções registradas', care.evolucoesRegistradas],
    ['Medicações aplicadas', care.medicacoesAplicadas],
    ['Alimentações registradas', care.alimentacoesRegistradas],
    ['Jejuns iniciados', care.jejunsIniciados],
    ['Refeições agendadas', care.refeicoesAgendadas],
    ['Refeições concluídas', care.refeicoesConcluidas],
    ['Refeições pendentes', care.refeicoesPendentes],
    ['Refeições canceladas', care.refeicoesCanceladas],
  ] as const

  return (
    <>
      <section className="report-admissions" aria-labelledby="report-admissions-title">
        <h2 id="report-admissions-title" className="report-section-title">Internações</h2>
        <p className="report-section-note">Os status e o tempo médio consideram as internações abertas no período. Os encerramentos são contados pela data de saída.</p>
        <dl className="clinic-stats report-stats">
          {stats.map(({ label, value, icon: Icon, unit }) => (
            <div className="clinic-stat report-stat" key={label}>
              <dt>{label}<Icon size={17} aria-hidden="true" /></dt>
              <dd>{number.format(value)}{unit && <span> {unit}</span>}</dd>
            </div>
          ))}
        </dl>
      </section>
      <section className="clinic-panel report-care" aria-labelledby="report-care-title">
        <h2 id="report-care-title" className="report-section-title"><Stethoscope size={18} aria-hidden="true" /> Cuidados clínicos</h2>
        <dl className="report-care-stats">
          {careStats.map(([label, value]) => (
            <div key={label}>
              <dt>{label}</dt>
              <dd>{number.format(value)}</dd>
            </div>
          ))}
        </dl>
      </section>
    </>
  )
}

function ReportDetails({ report }: { report: ClinicalReport }) {
  return (
    <>
      <ReportTable
        title="Atendimentos por veterinário"
        headings={['Veterinário', 'Internações', 'Ativas', 'Altas', 'Óbitos', 'Evoluções']}
        empty="Nenhum veterinário encontrado para estes filtros."
        rows={report.atendimentosPorVeterinario.map(vet => (
          <tr key={vet.veterinarioId}>
            <th scope="row">{vet.veterinarioNome}<small className="clinic-cell-detail">{vet.veterinarioEmail}</small></th>
            <td><strong>{number.format(vet.totalInternacoes)}</strong></td>
            <td>{number.format(vet.ativas)}</td>
            <td>{number.format(vet.altas)}</td>
            <td>{number.format(vet.obitos)}</td>
            <td>{number.format(vet.evolucoesRegistradas)}</td>
          </tr>
        ))}
      />
      <ReportTable
        title="Produtividade da equipe"
        headings={['Profissional', 'Perfil', 'Evoluções', 'Medicações', 'Alimentações', 'Total de ações']}
        empty="Nenhuma ação clínica registrada para estes filtros."
        rows={report.produtividadeEquipe.map(member => (
          <tr key={member.usuarioId}>
            <th scope="row">{member.nome}<small className="clinic-cell-detail">{member.email}</small></th>
            <td>{roleLabels[member.role]}</td>
            <td>{number.format(member.evolucoesRegistradas)}</td>
            <td>{number.format(member.medicacoesAplicadas)}</td>
            <td>{number.format(member.alimentacoesRegistradas)}</td>
            <td><strong>{number.format(member.totalAcoesClinicas)}</strong></td>
          </tr>
        ))}
      />
      <ReportTable
        title="Utilização das baias"
        headings={['Baia', 'Status atual', 'Internações no período']}
        empty="Nenhuma baia cadastrada."
        rows={report.utilizacaoBaias.map(baia => (
          <tr key={baia.baiaId}>
            <th scope="row">{baia.identificacao}</th>
            <td><StatusBadge status={baia.statusAtual} /></td>
            <td><strong>{number.format(baia.totalInternacoesNoPeriodo)}</strong></td>
          </tr>
        ))}
      />
    </>
  )
}

function ReportTable({ title, headings, empty, rows }: {
  title: string
  headings: string[]
  empty: string
  rows: ReactElement[]
}) {
  const id = useId()
  return (
    <section className="clinic-panel report-table-panel" aria-labelledby={id}>
      <h2 id={id}>{title}</h2>
      {rows.length ? (
        <div className="clinic-table-scroll" tabIndex={0}>
          <table className="clinic-table report-table" aria-labelledby={id}>
            <thead>
              <tr>{headings.map(heading => <th key={heading} scope="col">{heading}</th>)}</tr>
            </thead>
            <tbody>{rows}</tbody>
          </table>
        </div>
      ) : <p className="report-table-empty">{empty}</p>}
      {rows.length > 0 && headings.length > 3 && <p className="report-table-hint">Role para ver todas as colunas.</p>}
    </section>
  )
}
