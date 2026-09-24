export type Role = 'ADMIN' | 'VETERINARIO' | 'RECEPCIONISTA' | 'AUXILIAR_TECNICO' | 'TUTOR'

export type User = {
  id: number
  name: string
  email: string
  role: Role
  active: boolean
  confirmed: boolean
  lastActivityAt: string | null
  createdAt: string
  updatedAt: string
}

export type PageResponse<T> = {
  items: T[]
  page: number
  size: number
  total: number
  pages: number
}

type ErrorPayload = {
  message?: string
  fields?: Record<string, string>
}

type RequestOptions = Omit<RequestInit, 'body'> & {
  body?: unknown
  retryAuthentication?: boolean
  authenticated?: boolean
}

const API_URL = (import.meta.env.VITE_API_URL ?? '/qrvet/api/v1').replace(/\/$/, '')
const MUTATING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

let accessToken: string | null = null
let csrf: { token: string; headerName: string } | null = null
let refreshInProgress: Promise<void> | null = null
let csrfInProgress: Promise<{ token: string; headerName: string }> | null = null

const translations: Record<string, string> = {
  'Invalid CPF.': 'CPF inválido. Confira os números informados.',
  'A tutor with this CPF already exists.': 'Já existe um tutor com este CPF.',
  'A tutor with this email already exists.': 'Já existe um tutor com este e-mail.',
  'Invalid credentials': 'E-mail ou senha inválidos.',
  'Invalid current password': 'A senha atual está incorreta.',
  'Invalid session': 'Sua sessão expirou. Entre novamente.',
  'User not found': 'Usuário não encontrado.',
  'User not found.': 'Usuário não encontrado.',
  'Patient not found.': 'Paciente não encontrado.',
  'Tutor not found.': 'Tutor não encontrado.',
  'Hospitalization not found.': 'Internação não encontrada. Confira o código informado.',
  'Bay not found.': 'Baia não encontrada.',
  'Active veterinarian not found.': 'Selecione um veterinário ativo.',
  'The bay is not available.': 'Esta baia não está mais disponível. Atualize a lista.',
  'The patient already has an active hospitalization.': 'Este paciente já está internado.',
  'The hospitalization has already been closed.': 'Esta internação já foi encerrada.',
  'Food cannot be recorded while the hospitalization has active fasting.': 'Paciente em jejum. Encerre o jejum antes de registrar alimentação.',
  'Food can only be recorded for an active hospitalization.': 'A internação foi encerrada. Não é possível registrar alimentação.',
  'Fasting can only be changed for an active hospitalization.': 'A internação foi encerrada. Não é possível alterar o jejum.',
  'The hospitalization already has an active fasting period.': 'Este paciente já está em jejum.',
  'Active fasting period not found.': 'Não há jejum ativo para encerrar.',
  'User with this email already exists.': 'Já existe um usuário com este e-mail.',
  'Sign up already confirmed.': 'Este convite já foi aceito.',
  'Wait one minute before resending the invitation.': 'Aguarde um minuto para reenviar o convite.',
  'Invite already used or expired.': 'Convite já utilizado ou expirado. Solicite um novo ao administrador.',
  'The invitation could not be sent. Check the email service and try again.': 'Não foi possível enviar o convite. Tente novamente mais tarde.',
  'Password recovery link is invalid or expired. Request a new password recovery.': 'Link inválido ou expirado. Solicite uma nova recuperação de senha.',
}

export class ApiError extends Error {
  status: number
  fields: Record<string, string>

  constructor(status: number, message: string, fields: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fields = fields
  }
}

async function responseError(response: Response) {
  let payload: ErrorPayload = {}
  try {
    payload = await response.json() as ErrorPayload
  } catch {
    // Algumas respostas do Spring não possuem corpo JSON.
  }

  const fieldMessage = Object.values(payload.fields ?? {})[0]
  const fallback = response.status === 401
    ? 'E-mail ou senha inválidos.'
    : response.status === 403
      ? 'Você não tem permissão para realizar esta ação.'
      : 'Não foi possível concluir a solicitação.'

  const message = payload.message ?? ''
  return new ApiError(response.status, translations[message] ?? fieldMessage ?? (message || fallback), payload.fields)
}

async function ensureCsrf() {
  if (csrf) return csrf

  if (!csrfInProgress) csrfInProgress = (async () => {
    const response = await fetch(`${API_URL}/auth/csrf`, { credentials: 'include' })
    if (!response.ok) throw await responseError(response)
    csrf = await response.json() as { token: string; headerName: string }
    return csrf
  })().finally(() => { csrfInProgress = null })
  return csrfInProgress
}

async function rawRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase()
  const headers = new Headers(options.headers)

  if (options.body !== undefined) headers.set('Content-Type', 'application/json')
  if (accessToken && options.authenticated !== false) headers.set('Authorization', `Bearer ${accessToken}`)

  if (MUTATING_METHODS.has(method)) {
    const csrfData = await ensureCsrf()
    headers.set(csrfData.headerName, csrfData.token)
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    method,
    headers,
    credentials: 'include',
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  })

  if (!response.ok) throw await responseError(response)
  if (response.status === 204) return undefined as T
  const text = await response.text()
  return text ? JSON.parse(text) as T : undefined as T
}

async function refreshAccessToken() {
  if (!refreshInProgress) {
    refreshInProgress = rawRequest<{ accessToken: string }>('/auth/refresh', {
      method: 'POST',
      retryAuthentication: false,
      authenticated: false,
    }).then(({ accessToken: token }) => {
      accessToken = token
    }).finally(() => {
      refreshInProgress = null
    })
  }
  return refreshInProgress
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  try {
    return await rawRequest<T>(path, options)
  } catch (error) {
    if (error instanceof ApiError && error.status === 401 && options.retryAuthentication !== false) {
      try { await refreshAccessToken() } catch (refreshError) {
        if (refreshError instanceof ApiError && refreshError.status === 401) {
          accessToken = null
          window.dispatchEvent(new Event('qrvet:session-expired'))
        }
        throw refreshError
      }
      return rawRequest<T>(path, { ...options, retryAuthentication: false })
    }
    throw error
  }
}

export function readableError(error: unknown) {
  if (error instanceof ApiError) return error.message
  if (error instanceof TypeError) return 'Não foi possível conectar ao servidor.'
  return 'Ocorreu um erro inesperado. Tente novamente.'
}

export const authApi = {
  async login(email: string, password: string) {
    const response = await rawRequest<{ accessToken: string }>('/auth/login', {
      method: 'POST',
      body: { email, password },
      retryAuthentication: false,
      authenticated: false,
    })
    accessToken = response.accessToken
    csrf = null
  },
  async restore() {
    await refreshAccessToken()
    return request<User>('/auth/me', { retryAuthentication: false })
  },
  me: () => request<User>('/auth/me'),
  async logout() {
    try {
      await rawRequest<void>('/auth/logout', { method: 'POST', retryAuthentication: false, authenticated: false })
    } finally {
      accessToken = null
      csrf = null
    }
  },
  forgotPassword: (email: string) => rawRequest<{ message: string }>('/auth/forgot-password', {
    method: 'POST', body: { email }, retryAuthentication: false, authenticated: false,
  }),
  resetPassword: (token: string, newPassword: string) => rawRequest<void>('/auth/reset-password', {
    method: 'POST', body: { token, newPassword }, retryAuthentication: false, authenticated: false,
  }),
  confirmInvitation: (token: string, newPassword: string) => rawRequest<void>('/auth/confirm-invitation', {
    method: 'POST', body: { token, newPassword }, retryAuthentication: false, authenticated: false,
  }),
  changePassword: (currentPassword: string, newPassword: string) => request<void>('/auth/password', {
    method: 'PUT', body: { currentPassword, newPassword }, retryAuthentication: false,
  }),
  heartbeat: () => request<void>('/auth/heartbeat', { method: 'POST' }),
  clearLocalSession: () => { accessToken = null; csrf = null },
}

export const usersApi = {
  list: () => request<PageResponse<User>>('/users?page=0&size=100'),
  create: (data: { name: string; email: string; role: Role }) => request<User>('/users', {
    method: 'POST', body: data,
  }),
  resendInvitation: (id: number) => request<void>(`/users/${id}/invitation`, { method: 'POST' }),
}
