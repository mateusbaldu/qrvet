import { useEffect, useState } from 'react'
import { ChevronLeft, ChevronRight, LogOut, Menu, PawPrint, UserRound, Users, X } from 'lucide-react'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import '../styles/layout.css'

const titulos: Record<string, string> = {
  '/equipe': 'Equipe',
  '/meu-perfil': 'Meu perfil',
}

export function AppLayout() {
  const [menuAberto, setMenuAberto] = useState(false)
  const [menuRecolhido, setMenuRecolhido] = useState(false)
  const location = useLocation()
  const tituloAtual = titulos[location.pathname] ?? 'QRVet'

  useEffect(() => setMenuAberto(false), [location.pathname])

  return (
    <div className="app-shell">
      <aside className={`app-sidebar ${menuAberto ? 'open' : ''} ${menuRecolhido ? 'collapsed' : ''}`} aria-label="Menu principal">
        <div className="sidebar-heading">
          <Link to="/equipe" className="sidebar-brand">
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
          <NavLink to="/equipe" title={menuRecolhido ? 'Equipe' : undefined} className={({ isActive }) => isActive ? 'active' : ''}>
            <Users size={19} /> <span className="sidebar-link-text">Equipe</span>
          </NavLink>
          <NavLink to="/meu-perfil" title={menuRecolhido ? 'Meu perfil' : undefined} className={({ isActive }) => isActive ? 'active' : ''}>
            <UserRound size={19} /> <span className="sidebar-link-text">Meu perfil</span>
          </NavLink>
        </nav>

        <div className="sidebar-user mt-auto">
          <span className="sidebar-avatar">DS</span>
          <span><strong>Dra. Sarah</strong><small>Administradora</small></span>
          <Link to="/login" aria-label="Sair"><LogOut size={18} /></Link>
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
            <span className="header-profile-text"><strong>Dra. Sarah Jenkins</strong><small>Administradora</small></span>
            <span className="header-avatar">DS</span>
          </Link>
        </header>

        <div className="app-content"><Outlet /></div>
      </div>
    </div>
  )
}
