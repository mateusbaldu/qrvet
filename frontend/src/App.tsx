import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/useAuth'
import { ForgotPassword } from './pages/ForgotPassword'
import { Login } from './pages/Login'
import { ResetPassword } from './pages/ResetPassword'
import { Team } from './pages/Team'
import { Profile } from './pages/Profile'
import { AppLayout } from './components/AppLayout'
import { AdminRoute, ProtectedRoute, RoleRoute } from './auth/RouteGuards'
import { Registry, PatientDetail } from './pages/Registry'
import { Dashboard } from './pages/Dashboard'
import { Hospitalizations, HospitalizationDetail, CareLookup } from './pages/Hospitalizations'
import { PublicHospitalization, QrLookup, Sessions, Bootstrap } from './pages/ClinicAccess'

export default function App() {
  return (
    <BrowserRouter> 
      <Routes>
        <Route path="/public/internacoes/qr/:token" element={<PublicHospitalization />} />
        <Route path="/configuracao-inicial" element={<Bootstrap />} />
        <Route path="/login" element={<Login />} />
        <Route path="/esqueci-senha" element={<ForgotPassword />} />
        <Route path="/redefinir-senha" element={<ResetPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="/registro" element={<ResetPassword invitation />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/" element={<HomeRedirect />} />
            <Route element={<AdminRoute />}>
              <Route path="/equipe" element={<Team />} />
              <Route path="/sessoes" element={<Sessions />} />
            </Route>
            <Route element={<RoleRoute roles={['ADMIN', 'VETERINARIO', 'RECEPCIONISTA']} />}>
              <Route path="/inicio" element={<Dashboard />} />
              <Route path="/tutores" element={<Registry key="tutores" kind="tutores" />} />
              <Route path="/pacientes" element={<Registry key="pacientes" kind="pacientes" />} />
              <Route path="/pacientes/:id" element={<PatientDetail />} />
              <Route path="/baias" element={<Registry key="baias" kind="baias" />} />
              <Route path="/internacoes" element={<Hospitalizations />} />
              <Route path="/internacoes/:id" element={<HospitalizationDetail />} />
            </Route>
            <Route element={<RoleRoute roles={['ADMIN', 'VETERINARIO', 'AUXILIAR_TECNICO']} />}>
              <Route path="/cuidados" element={<CareLookup />} />
            </Route>
            <Route path="/consultar-qr" element={<QrLookup />} />
            <Route path="/meu-perfil" element={<Profile />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

function HomeRedirect() {
  const { user } = useAuth()
  return <Navigate to={user && ['ADMIN', 'VETERINARIO', 'RECEPCIONISTA'].includes(user.role) ? '/inicio' : user?.role === 'AUXILIAR_TECNICO' ? '/cuidados' : '/consultar-qr'} replace />
}
