import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './useAuth'
import type { Role } from '../services/api'
import { routeRoles } from '../routes'

export function RoleRoute({ roles }: { roles?: Role[] }) {
  const { user } = useAuth()

  if (user && (roles ?? []).includes(user.role)) {
    return <Outlet />
  }

  return <Navigate to="/meu-perfil" replace />
}

export function RouteAccess({ path }: { path: string }) {
  return <RoleRoute roles={routeRoles(path)} />
}

function LoadingSession() {
  return (
    <div className="session-loading" role="status">
      Carregando sua sessão...
    </div>
  )
}

export function ProtectedRoute() {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <LoadingSession />
  if (!user)
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  return <Outlet />
}

export function AdminRoute() {
  const { user } = useAuth()
  if (user?.role !== 'ADMIN') return <Navigate to="/meu-perfil" replace />
  return <Outlet />
}
