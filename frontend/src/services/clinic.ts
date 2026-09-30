import { request, type PageResponse } from './api'
export type Tutor = { id: number; nome: string; cpf: string; telefone: string; email: string; endereco: string }
export type Paciente = { id: number; tutorId: number; nome: string; especie: string; raca: string; sexo: string; dataNascimento: string; peso: number; observacoes: string }
export type Baia = { id: number; identificacao: string; status: 'DISPONIVEL' | 'OCUPADA' | 'MANUTENCAO'; observacao: string }
export type Internacao = { id: number; pacienteId: number; pacienteNome: string; baiaId: number; baiaIdentificacao: string; veterinarioId: number; veterinarioNome: string; uuidToken: string; entradaInternacao: string; saidaInternacao: string | null; motivo: string; diagnosticoInicial: string; status: 'ATIVA' | 'ALTA' | 'OBITO'; observacoes: string }
export type Veterinario = { id: number; name: string }
export type Cuidado = Pick<Internacao, 'id' | 'pacienteNome' | 'baiaIdentificacao' | 'status'>
export type Jejum = { id: number; motivo: string; dataHoraInicio: string; dataHoraFim: string | null; ativo: boolean }
export type Alimentacao = { id: number; alimento: string; quantidade: string; aceitacaoObservacao: string; dataHoraRegistro: string; usuarioId: number }
export type PublicInternacao = { pacienteNome: string; especie: string; raca: string; sexo: string; baiaIdentificacao: string; motivo: string; status: string; jejumAtivo: boolean; entradaInternacao: string }
export type QrCode = { uuidToken: string; url: string; base64: string }
export type Session = { jti: string; createdAt: string; expiresAt: string }
export async function allPages<T>(path: string): Promise<T[]> {
  const items: T[] = []
  let page = 0
  while (true) {
    const result = await request<PageResponse<T>>(`${path}${path.includes('?') ? '&' : '?'}page=${page}&size=100`)
    items.push(...result.items)
    if (++page >= result.pages) return items
  }
}
export const clinicApi = {
  list: <T,>(path: string, page = 0, filters = '') => request<PageResponse<T>>(`${path}?page=${page}&size=12${filters}`),
  save: <T,>(path: string, body: unknown, method = 'POST') => request<T>(path, { method, body }),
  publicDetails: (token: string) => request<PublicInternacao>(`/public/internacoes/qr/${encodeURIComponent(token)}`, { retryAuthentication: false, authenticated: false }),
}
export const dateTime = (value: string | null) => value && !Number.isNaN(Date.parse(value)) ? new Date(value).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' }) : '—'
export const dateOnly = (value: string) => value ? value.split('-').reverse().join('/') : '—'
export const searchText = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('pt-BR')
export function paginate<T>(items: T[], page: number, size = 12) {
  const pages = Math.ceil(items.length / size)
  const current = Math.min(page, Math.max(0, pages - 1))
  return { items: items.slice(current * size, (current + 1) * size), page: current, pages, total: items.length, size }
}
export const labels: Record<string, string> = { DISPONIVEL: 'Disponível', OCUPADA: 'Ocupada', MANUTENCAO: 'Manutenção', ATIVA: 'Ativa', ALTA: 'Alta', OBITO: 'Óbito' }
