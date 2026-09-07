export type Role = 'ADMIN' | 'VETERINARIAN' | 'RECEPTIONIST'

export type User = {
  id: string
  name: string
  email: string
  phone: string | null
  avatarUrl: string | null
  role: Role
  crmv: string | null
  clinic: { id: string; name: string }
}

export type AuthResponse = {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: User
}

type ErrorPayload = {
  message?: string
  code?: string
  fieldErrors?: Record<string, string> | null
}

export class ApiError extends Error {
  status: number
  code: string
  fieldErrors: Record<string, string>

  constructor(status: number, payload: ErrorPayload) {
    super(payload.message || 'Não foi possível concluir a solicitação.')
    this.name = 'ApiError'
    this.status = status
    this.code = payload.code || 'UNKNOWN_ERROR'
    this.fieldErrors = payload.fieldErrors || {}
  }
}

let accessToken: string | null = null
let refreshPromise: Promise<AuthResponse> | null = null

function saveSession(response: AuthResponse) {
  accessToken = response.accessToken
  return response
}

async function readResponse<T>(response: Response): Promise<T> {
  if (response.status === 204) return undefined as T

  const payload = await response.json().catch(() => ({})) as T & ErrorPayload
  if (!response.ok) throw new ApiError(response.status, payload)
  return payload
}

async function rawRequest<T>(path: string, options: RequestInit = {}) {
  const headers = new Headers(options.headers)
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json')

  return readResponse<T>(await fetch(`/api${path}`, {
    ...options,
    headers,
    credentials: 'include',
  }))
}

export async function refreshSession() {
  if (!refreshPromise) {
    refreshPromise = rawRequest<AuthResponse>('/auth/refresh', { method: 'POST' })
      .then(saveSession)
      .finally(() => { refreshPromise = null })
  }
  return refreshPromise
}

export async function apiRequest<T>(path: string, options: RequestInit = {}, retry = true): Promise<T> {
  try {
    return await rawRequest<T>(path, options)
  } catch (error) {
    if (retry && error instanceof ApiError && error.status === 401 && path !== '/auth/refresh') {
      try {
        await refreshSession()
        return await apiRequest<T>(path, options, false)
      } catch {
        accessToken = null
        window.dispatchEvent(new Event('qrvet:unauthorized'))
      }
    }
    throw error
  }
}

export async function loginRequest(email: string, password: string) {
  return saveSession(await rawRequest<AuthResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  }))
}

export async function logoutRequest() {
  try {
    await rawRequest<void>('/auth/logout', { method: 'POST' })
  } finally {
    accessToken = null
  }
}

export function roleLabel(role: Role) {
  if (role === 'ADMIN') return 'Administrador(a)'
  if (role === 'VETERINARIAN') return 'Veterinário(a)'
  return 'Recepcionista'
}

export function initials(name: string) {
  return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
}

export function errorMessage(error: unknown) {
  return error instanceof ApiError ? error.message : 'Não foi possível se conectar ao servidor.'
}
