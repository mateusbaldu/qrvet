import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './useAuth'

function LoadingSession() {
  return <div className="session-loading" role="status">Carregando sua sessão...</div>
}

export function ProtectedRoute() {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <LoadingSession />
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}

export function AdminRoute() {
  const { user } = useAuth()
  if (user?.role !== 'ADMIN') return <Navigate to="/meu-perfil" replace />
  return <Outlet />
}
