import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { ForgotPassword } from './pages/ForgotPassword'
import { Login } from './pages/Login'
import { ResetPassword } from './pages/ResetPassword'
import { Team } from './pages/Team'
import { Profile } from './pages/Profile'
import { AppLayout } from './components/AppLayout'
import { AuthProvider } from './auth/AuthProvider'
import { ProtectedRoute, PublicOnlyRoute } from './auth/RouteGuards'
import { AcceptInvitation } from './pages/AcceptInvitation'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
        <Route element={<PublicOnlyRoute />}>
          <Route path="/login" element={<Login />} />
        </Route>
        <Route path="/esqueci-senha" element={<ForgotPassword />} />
        <Route path="/redefinir-senha" element={<ResetPassword />} />
        <Route path="/aceitar-convite" element={<AcceptInvitation />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/equipe" element={<Team />} />
            <Route path="/meu-perfil" element={<Profile />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
