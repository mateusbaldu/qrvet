import { request, type PageResponse } from './api'
export type Tutor = { id: number; nome: string; cpf: string; telefone: string; email: string; endereco: string }
export type Paciente = { id: number; tutorId: number; nome: string; especie: string; raca: string; sexo: string; dataNascimento: string; peso: number; observacoes: string }
export type Baia = { id: number; identificacao: string; status: 'DISPONIVEL' | 'OCUPADA' | 'MANUTENCAO'; observacao: string }
export type Internacao = { id: number; pacienteId: number; baiaId: number; veterinarioId: number; uuidToken: string; entradaInternacao: string; saidaInternacao: string | null; motivo: string; diagnosticoInicial: string; status: 'ATIVA' | 'ALTA' | 'OBITO'; observacoes: string }
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
  publicDetails: (token: string) => request<PublicInternacao>(`/public/internacoes/qr/${encodeURIComponent(token)}`, { retryAuthentication: false }),
}
export const dateTime = (value: string | null) => value ? new Date(value).toLocaleString('pt-BR') : '—'
export const labels: Record<string, string> = { DISPONIVEL: 'Disponível', OCUPADA: 'Ocupada', MANUTENCAO: 'Manutenção', ATIVA: 'Ativa', ALTA: 'Alta', OBITO: 'Óbito' }
