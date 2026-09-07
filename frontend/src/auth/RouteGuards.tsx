import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './auth-context'

function SessionLoading() {
  return <main className="session-loading" aria-live="polite"><span className="spinner-border" /> Verificando sua sessão...</main>
}

export function ProtectedRoute() {
  const { user, checkingSession } = useAuth()
  const location = useLocation()
  if (checkingSession) return <SessionLoading />
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}

export function PublicOnlyRoute() {
  const { user, checkingSession } = useAuth()
  if (checkingSession) return <SessionLoading />
  if (user) return <Navigate to="/equipe" replace />
  return <Outlet />
}
