import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { ForgotPassword } from './pages/ForgotPassword'
import { Login } from './pages/Login'
import { ResetPassword } from './pages/ResetPassword'
import { Team } from './pages/Team'
import { Profile } from './pages/Profile'
import { AppLayout } from './components/AppLayout'

export default function App() {
  return (
    <BrowserRouter> 
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/esqueci-senha" element={<ForgotPassword />} />
        <Route path="/redefinir-senha" element={<ResetPassword />} />
        <Route element={<AppLayout />}>
          <Route path="/equipe" element={<Team />} />
          <Route path="/meu-perfil" element={<Profile />} />
        </Route>
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
