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
}

const API_URL = (import.meta.env.VITE_API_URL ?? '/qrvet/api/v1').replace(/\/$/, '')
const MUTATING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

let accessToken: string | null = null
let csrf: { token: string; headerName: string } | null = null
let refreshInProgress: Promise<void> | null = null

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

  return new ApiError(response.status, fieldMessage ?? payload.message ?? fallback, payload.fields)
}

async function ensureCsrf() {
  if (csrf) return csrf

  const response = await fetch(`${API_URL}/auth/csrf`, { credentials: 'include' })
  if (!response.ok) throw await responseError(response)
  csrf = await response.json() as { token: string; headerName: string }
  return csrf
}

async function rawRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase()
  const headers = new Headers(options.headers)

  if (options.body !== undefined) headers.set('Content-Type', 'application/json')
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)

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
  return await response.json() as T
}

async function refreshAccessToken() {
  if (!refreshInProgress) {
    refreshInProgress = rawRequest<{ accessToken: string }>('/auth/refresh', {
      method: 'POST',
      retryAuthentication: false,
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
      await refreshAccessToken()
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
    })
    accessToken = response.accessToken
  },
  async restore() {
    await refreshAccessToken()
    return request<User>('/auth/me', { retryAuthentication: false })
  },
  me: () => request<User>('/auth/me'),
  async logout() {
    try {
      await rawRequest<void>('/auth/logout', { method: 'POST', retryAuthentication: false })
    } finally {
      accessToken = null
      csrf = null
    }
  },
  forgotPassword: (email: string) => rawRequest<{ message: string }>('/auth/forgot-password', {
    method: 'POST', body: { email }, retryAuthentication: false,
  }),
  resetPassword: (token: string, newPassword: string) => rawRequest<void>('/auth/reset-password', {
    method: 'POST', body: { token, newPassword }, retryAuthentication: false,
  }),
  confirmInvitation: (token: string, newPassword: string) => rawRequest<void>('/auth/confirm-invitation', {
    method: 'POST', body: { token, newPassword }, retryAuthentication: false,
  }),
  changePassword: (currentPassword: string, newPassword: string) => request<void>('/auth/password', {
    method: 'PUT', body: { currentPassword, newPassword },
  }),
  heartbeat: () => request<void>('/auth/heartbeat', { method: 'POST' }),
  clearLocalSession: () => { accessToken = null },
}

export const usersApi = {
  list: () => request<PageResponse<User>>('/users?page=0&size=100'),
  create: (data: { name: string; email: string; role: Role }) => request<User>('/users', {
    method: 'POST', body: data,
  }),
  resendInvitation: (id: number) => request<void>(`/users/${id}/invitation`, { method: 'POST' }),
}
