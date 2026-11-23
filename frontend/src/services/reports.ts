import { request, type Role } from './api'
import { toEndOfDayIso, toStartOfDayIso, type Baia } from './clinic'

export type ReportFilters = {
  startDate: string
  endDate: string
  veterinarioId: string
}

export type ClinicalReport = {
  periodo: {
    startDate: string | null
    endDate: string | null
    veterinarioId: number | null
  }
  resumoInternacoes: {
    totalNoPeriodo: number
    ativas: number
    altas: number
    obitos: number
    encerradasNoPeriodo: number
    tempoMedioInternacaoHoras: number
  }
  resumoCuidados: {
    evolucoesRegistradas: number
    medicacoesAplicadas: number
    alimentacoesRegistradas: number
    refeicoesAgendadas: number
    refeicoesConcluidas: number
    refeicoesPendentes: number
    refeicoesCanceladas: number
    jejunsIniciados: number
  }
  atendimentosPorVeterinario: {
    veterinarioId: number
    veterinarioNome: string
    veterinarioEmail: string
    totalInternacoes: number
    ativas: number
    altas: number
    obitos: number
    evolucoesRegistradas: number
  }[]
  produtividadeEquipe: {
    usuarioId: number
    nome: string
    email: string
    role: Role
    evolucoesRegistradas: number
    medicacoesAplicadas: number
    alimentacoesRegistradas: number
    totalAcoesClinicas: number
  }[]
  utilizacaoBaias: {
    baiaId: number
    identificacao: string
    statusAtual: Baia['status']
    totalInternacoesNoPeriodo: number
  }[]
}

export function clinicalReportQuery(filters: ReportFilters) {
  const query = new URLSearchParams()
  const start = toStartOfDayIso(filters.startDate)
  const end = toEndOfDayIso(filters.endDate)
  if (start) query.set('startDate', start)
  if (end) query.set('endDate', end)
  if (filters.veterinarioId) query.set('veterinarioId', filters.veterinarioId)
  return query.size ? '?' + query.toString() : ''
}

export const reportsApi = {
  clinical: (query: string) => request<ClinicalReport>('/relatorios/clinica' + query),
  exportClinical: (query: string) => {
    const params = new URLSearchParams(query)
    params.set('fuso', Intl.DateTimeFormat().resolvedOptions().timeZone)
    return request<Blob>('/relatorios/clinica/export/xlsx?' + params, { responseType: 'blob' })
  },
}
