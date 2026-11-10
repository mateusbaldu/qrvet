import { useEffect, useRef, useState } from 'react'
import { ChevronLeft, ChevronRight, CircleHelp, HeartPulse, House, LayoutDashboard, LogOut, Menu, PawPrint, QrCode, ShieldCheck, Stethoscope, UserRound, Users, Utensils, X } from 'lucide-react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import type { Role } from '../services/api'
import { appRoutes, adminRoutes } from '../routes'
import { Onboarding } from './Onboarding'
import '../styles/layout.css'

const roleLabels: Record<Role, string> = {
  ADMIN: 'Administrador(a)', VETERINARIO: 'Veterinário(a)', RECEPCIONISTA: 'Recepcionista', AUXILIAR_TECNICO: 'Auxiliar técnico(a)', TUTOR: 'Tutor(a)',
}
const initials = (name: string) => name.trim().split(/\s+/).slice(0, 2).map(part => part[0]?.toUpperCase()).join('') || '—'
const navIcons: Record<string, typeof House> = {
  '/inicio': LayoutDashboard, '/internacoes': HeartPulse, '/pacientes': PawPrint, '/tutores': Users, '/baias': House,
  '/alimentacao': Utensils, '/cuidados': Stethoscope, '/sessoes': ShieldCheck, '/auditoria': ShieldCheck, '/consultar-qr': QrCode, '/equipe': Users,
}
const groups = [
  { label: 'Rotina clínica', paths: ['/inicio', '/internacoes', '/alimentacao', '/cuidados'] },
  { label: 'Cadastros e acesso', paths: ['/pacientes', '/tutores', '/baias', '/consultar-qr'] },
  { label: 'Administração', paths: ['/equipe', '/sessoes', '/auditoria'] },
]

export function AppLayout() {
  const [menuAberto, setMenuAberto] = useState(false)
  const [guideOpen, setGuideOpen] = useState(false)
  const [menuRecolhido, setMenuRecolhido] = useState(false)
  const menuButton = useRef<HTMLButtonElement>(null)
  const location = useLocation()
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  useEffect(() => {
    if (!menuAberto) return
    const close = (event: KeyboardEvent) => {
      if (event.key === 'Escape') { setMenuAberto(false); menuButton.current?.focus() }
    }
    window.addEventListener('keydown', close)
    return () => window.removeEventListener('keydown', close)
  }, [menuAberto])
  if (!user) return null
  const routes = [...appRoutes, ...adminRoutes]
  const currentPath = '/' + location.pathname.split('/')[1]
  const title = currentPath === '/meu-perfil' ? 'Meu perfil' : routes.find(route => route.path === currentPath)?.label ?? 'QRVet'
  async function sair() {
    if (!user) return
    const email = user.email
    try { await logout() } catch { /* Local logout remains available when the API is offline. */ }
    navigate('/login', { replace: true, state: { email } })
  }
  return (
    <div className="app-shell">
      <a className="skip-link" href="#conteudo">Pular para o conteúdo</a>
      <aside id="menu-principal" className={'app-sidebar' + (menuAberto ? ' open' : '') + (menuRecolhido ? ' collapsed' : '')} aria-label="Menu principal">
        <div className="sidebar-heading">
          <Link to="/" className="sidebar-brand"><span className="sidebar-logo"><PawPrint size={25} /></span><span><strong>QRVet<span className="brand-dot">.</span></strong><small>Gestão veterinária</small></span></Link>
          <button className="sidebar-close d-lg-none" type="button" onClick={() => setMenuAberto(false)} aria-label="Fechar menu"><X size={21} /></button>
        </div>
        <button className="sidebar-collapse d-none d-lg-grid" type="button" onClick={() => setMenuRecolhido(!menuRecolhido)} aria-label={menuRecolhido ? 'Expandir menu lateral' : 'Recolher menu lateral'} aria-expanded={!menuRecolhido}>{menuRecolhido ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}</button>
        <nav className="sidebar-nav" aria-label="Navegação do sistema">
          {groups.map(group => {
            const items = group.paths.map(path => routes.find(route => route.path === path)).filter(route => route && route.roles.includes(user.role))
            if (!items.length) return null
            return <div className="sidebar-nav-group" key={group.label}><span className="sidebar-section-label">{group.label}</span>
              {items.map(item => {
                if (!item) return null
                const Icon = navIcons[item.path]
                return <NavLink key={item.path} to={item.path} title={item.label} onClick={() => setMenuAberto(false)} className={({ isActive }) => isActive ? 'active' : ''}><Icon size={19} /><span className="sidebar-link-text">{item.label}</span><ChevronRight className="sidebar-active-arrow" size={14} /></NavLink>
              })}
            </div>
          })}
          <div className="sidebar-nav-group sidebar-account"><NavLink to="/meu-perfil" title="Meu perfil" onClick={() => setMenuAberto(false)} className={({ isActive }) => isActive ? 'active' : ''}><UserRound size={19} /><span className="sidebar-link-text">Meu perfil</span></NavLink></div>
        </nav>
        <div className="sidebar-user mt-auto"><span className="sidebar-avatar">{initials(user.name)}</span><span><strong>{user.name}</strong><small>{roleLabels[user.role]}</small></span><button type="button" onClick={() => void sair()} aria-label="Sair"><LogOut size={18} /></button></div>
      </aside>
      {menuAberto && <button className="sidebar-overlay d-lg-none" type="button" onClick={() => setMenuAberto(false)} aria-label="Fechar menu" />}
      <div className="app-main">
        <header className="app-header">
          <div className="app-header-title"><button ref={menuButton} className="app-menu-button d-lg-none" type="button" onClick={() => setMenuAberto(true)} aria-label="Abrir menu" aria-controls="menu-principal" aria-expanded={menuAberto}><Menu size={22} /></button><div><small>Área da clínica <ChevronRight size={12} /></small><strong>{title === 'Início' ? 'Painel inicial' : title}</strong></div></div>
          <div className="header-actions"><button type="button" className="onboarding-help-button" onClick={() => setGuideOpen(true)} aria-label="Ajuda: guia do QRVet"><CircleHelp size={18} aria-hidden="true" /><span>Ajuda</span></button><Link to="/consultar-qr" className="header-qr" aria-label="Consultar por QR"><QrCode size={18} /><span>Consultar por QR</span></Link><Link to="/meu-perfil" className="header-profile" aria-label="Abrir meu perfil"><span className="header-profile-text"><strong>{user.name}</strong><small>{roleLabels[user.role]}</small></span><span className="header-avatar">{initials(user.name)}</span></Link></div>
        </header>
        <div className="app-content" id="conteudo" tabIndex={-1}><Onboarding key={user.id + ':' + user.role} user={user} open={guideOpen} onOpen={() => setGuideOpen(true)} onClose={() => setGuideOpen(false)} /><Outlet /></div>
      </div>
    </div>
  )
}
