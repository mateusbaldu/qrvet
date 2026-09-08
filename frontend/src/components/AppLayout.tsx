import { useState } from 'react'
import { ChevronLeft, ChevronRight, LogOut, Menu, PawPrint, UserRound, Users, X } from 'lucide-react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import type { Role } from '../services/api'
import '../styles/layout.css'

const titulos: Record<string, string> = {
  '/equipe': 'Equipe',
  '/meu-perfil': 'Meu perfil',
}

const roleLabels: Record<Role, string> = { ADMIN: 'Administrador(a)', VETERINARIO: 'Veterinário(a)', RECEPCIONISTA: 'Recepcionista', AUXILIAR_TECNICO: 'Auxiliar técnico(a)', TUTOR: 'Tutor(a)' }
const initials = (name: string) => name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase()).join('') || '—'

export function AppLayout() {
  const [menuAberto, setMenuAberto] = useState(false)
  const [menuRecolhido, setMenuRecolhido] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const tituloAtual = titulos[location.pathname] ?? 'QRVet'

  if (!user) return null

  async function sair() {
    if (!user) return
    const email = user.email
    await logout()
    navigate('/login', { replace: true, state: { email } })
  }

  return (
    <div className="app-shell">
      <aside className={`app-sidebar ${menuAberto ? 'open' : ''} ${menuRecolhido ? 'collapsed' : ''}`} aria-label="Menu principal">
        <div className="sidebar-heading">
          <Link to={user.role === 'ADMIN' ? '/equipe' : '/meu-perfil'} className="sidebar-brand">
            <span className="sidebar-logo"><PawPrint size={25} /></span>
            <span><strong>QRVet</strong><small>Gestão veterinária</small></span>
          </Link>
          <button className="sidebar-close d-lg-none" type="button" onClick={() => setMenuAberto(false)} aria-label="Fechar menu">
            <X size={21} />
          </button>
        </div>

        <button
          className="sidebar-collapse d-none d-lg-grid"
          type="button"
          onClick={() => setMenuRecolhido((atual) => !atual)}
          aria-label={menuRecolhido ? 'Expandir menu lateral' : 'Recolher menu lateral'}
          aria-expanded={!menuRecolhido}
        >
          {menuRecolhido ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
        </button>

        <nav className="sidebar-nav">
          <span className="sidebar-section-label">CLÍNICA</span>
          {user.role === 'ADMIN' && <NavLink to="/equipe" onClick={() => setMenuAberto(false)} title={menuRecolhido ? 'Equipe' : undefined} className={({ isActive }) => isActive ? 'active' : ''}>
            <Users size={19} /> <span className="sidebar-link-text">Equipe</span>
          </NavLink>}
          <NavLink to="/meu-perfil" onClick={() => setMenuAberto(false)} title={menuRecolhido ? 'Meu perfil' : undefined} className={({ isActive }) => isActive ? 'active' : ''}>
            <UserRound size={19} /> <span className="sidebar-link-text">Meu perfil</span>
          </NavLink>
        </nav>

        <div className="sidebar-user mt-auto">
          <span className="sidebar-avatar">{initials(user.name)}</span>
          <span><strong>{user.name}</strong><small>{roleLabels[user.role]}</small></span>
          <button type="button" onClick={() => void sair()} aria-label="Sair"><LogOut size={18} /></button>
        </div>
      </aside>

      {menuAberto && <button className="sidebar-overlay d-lg-none" type="button" onClick={() => setMenuAberto(false)} aria-label="Fechar menu" />}

      <div className="app-main">
        <header className="app-header">
          <div className="app-header-title">
            <button className="app-menu-button d-lg-none" type="button" onClick={() => setMenuAberto(true)} aria-label="Abrir menu">
              <Menu size={22} />
            </button>
            <div><small>QRVet</small><strong>{tituloAtual}</strong></div>
          </div>

          <Link to="/meu-perfil" className="header-profile" aria-label="Abrir meu perfil">
            <span className="header-profile-text"><strong>{user.name}</strong><small>{roleLabels[user.role]}</small></span>
            <span className="header-avatar">{initials(user.name)}</span>
          </Link>
        </header>

        <div className="app-content"><Outlet /></div>
      </div>
    </div>
  )
}
