import type { Role } from './services/api'

export type AppRoute = {
  path: string
  label: string
  roles: Role[]
}

export const appRoutes: AppRoute[] = [
  { path: '/inicio', label: 'Início', roles: ['ADMIN', 'VETERINARIO', 'RECEPCIONISTA'] },
  { path: '/internacoes', label: 'Internações', roles: ['ADMIN', 'VETERINARIO', 'RECEPCIONISTA'] },
  { path: '/pacientes', label: 'Pacientes', roles: ['ADMIN', 'VETERINARIO', 'RECEPCIONISTA'] },
  { path: '/tutores', label: 'Tutores', roles: ['ADMIN', 'VETERINARIO', 'RECEPCIONISTA'] },
  { path: '/baias', label: 'Baias', roles: ['ADMIN', 'VETERINARIO', 'RECEPCIONISTA'] },
  { path: '/alimentacao', label: 'Alimentação', roles: ['ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO'] },
  { path: '/cuidados', label: 'Cuidados', roles: ['ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO'] },
  { path: '/sessoes', label: 'Sessões', roles: ['ADMIN'] },
  { path: '/auditoria', label: 'Auditoria', roles: ['ADMIN'] },
  {
    path: '/consultar-qr',
    label: 'Consultar QR Code',
    roles: ['ADMIN', 'VETERINARIO', 'RECEPCIONISTA', 'AUXILIAR_TECNICO', 'TUTOR'],
  },
]

export const adminRoutes: AppRoute[] = [
  { path: '/equipe', label: 'Equipe', roles: ['ADMIN'] },
]

export function routeRoles(path: string): Role[] {
  const directRoute = appRoutes.find(route => route.path === path)
  if (directRoute) {
    return directRoute.roles
  }
  if (path.startsWith('/internacoes/')) {
    return ['ADMIN', 'VETERINARIO', 'RECEPCIONISTA', 'AUXILIAR_TECNICO']
  }
  if (path.startsWith('/pacientes/')) {
    return routeRoles('/pacientes')
  }
  const adminRoute = adminRoutes.find(route => route.path === path)
  if (adminRoute) {
    return adminRoute.roles
  }
  return []
}