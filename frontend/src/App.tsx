import { BrowserRouter, Navigate, Route, Routes, useParams } from 'react-router-dom'
import { useAuth } from './auth/useAuth'
import { ForgotPassword } from './pages/ForgotPassword'
import { Login } from './pages/Login'
import { ResetPassword } from './pages/ResetPassword'
import { Team } from './pages/Team'
import { Profile } from './pages/Profile'
import { AppLayout } from './components/AppLayout'
import { AdminRoute, ProtectedRoute, RouteAccess } from './auth/RouteGuards'
import { Registry, PatientDetail } from './pages/Registry'
import { Feeding } from './pages/Feeding'
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
            <Route element={<RouteAccess path="/inicio" />}>
              <Route path="/inicio" element={<Dashboard />} />
            </Route>
            <Route element={<RouteAccess path="/tutores" />}>
              <Route path="/tutores" element={<Registry key="tutores" kind="tutores" />} />
            </Route>
            <Route element={<RouteAccess path="/pacientes" />}>
              <Route path="/pacientes" element={<Registry key="pacientes" kind="pacientes" />} />
              <Route path="/pacientes/:id" element={<PatientDetail />} />
            </Route>
            <Route element={<RouteAccess path="/baias" />}>
              <Route path="/baias" element={<Registry key="baias" kind="baias" />} />
            </Route>
            <Route element={<RouteAccess path="/internacoes" />}>
              <Route path="/internacoes" element={<Hospitalizations />} />
            </Route>
            <Route element={<RouteAccess path="/internacoes/:id" />}>
              <Route path="/internacoes/:id" element={<HospitalizationAccess />} />
            </Route>
            <Route element={<RouteAccess path="/alimentacao" />}>
              <Route path="/alimentacao" element={<Feeding />} />
            </Route>
            <Route element={<RouteAccess path="/cuidados" />}>
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

function HospitalizationAccess() {
  const { user } = useAuth()
  const { id = '' } = useParams()

  if (user?.role === 'AUXILIAR_TECNICO') {
    return <Navigate to={'/cuidados?internacao=' + encodeURIComponent(id)} replace />
  }

  return <HospitalizationDetail />
}

function HomeRedirect() {
  const { user } = useAuth()

  let destination = '/consultar-qr'

  if (user && ['ADMIN', 'VETERINARIO', 'RECEPCIONISTA'].includes(user.role)) {
    destination = '/inicio'
  } else if (user?.role === 'AUXILIAR_TECNICO') {
    destination = '/cuidados'
  }

  return <Navigate to={destination} replace />
}
