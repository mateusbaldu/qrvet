import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, CalendarDays, HeartPulse, House, PawPrint, Plus, QrCode, RefreshCw, Search, Stethoscope, Users, Utensils } from 'lucide-react'
import { useAuth } from '../auth/useAuth'
import { Page, ResourceState, EmptyState, StatusBadge } from '../components/ClinicUI'
import { useResource } from '../hooks/useResource'
import { allPages, clinicApi, dateTime, searchText, type Baia, type Internacao } from '../services/clinic'
import { FeedingSummary } from './FeedingSummary'
import { PatientAvatar } from '../components/PatientEditor'
import '../styles/dashboard.css'

export function Dashboard() {
  const { user } = useAuth()
  const [search, setSearch] = useState('')
  const [onlyMine, setOnlyMine] = useState(false)
  const resource = useResource(async () => {
    const [patients, tutors, bays, admissions] = await Promise.all([
      clinicApi.list('/pacientes'), clinicApi.list('/tutores'),
      allPages<Baia>('/baias'), allPages<Internacao>('/internacoes/ativas'),
    ])
    return { patients: patients.total, tutors: tutors.total, bays,
      availableBays: bays.filter(b => b.status === 'DISPONIVEL').length,
      admissions: admissions.sort((a, b) => Date.parse(b.entradaInternacao) - Date.parse(a.entradaInternacao)),
    }
  }, ['dashboard'])
  const data = resource.data
  const clinical = user?.role === 'ADMIN' || user?.role === 'VETERINARIO'
  const admissions = data?.admissions.filter(i =>
    (!onlyMine || i.veterinarioId === user?.id) &&
    searchText([i.pacienteNome, i.baiaIdentificacao, i.veterinarioNome].join(' ')).includes(searchText(search.trim()))
  ) ?? []
  const shortcuts = [
    { path: '/pacientes?novo=1', title: 'Cadastrar paciente', text: 'Paciente e tutor em uma etapa', icon: PawPrint },
    ...(clinical ? [
      { path: '/cuidados', title: 'Registrar cuidados', text: 'Medicação, alimentação e jejum', icon: Stethoscope },
      { path: '/alimentacao', title: 'Checklist de alimentação', text: 'Horários e refeições do dia', icon: Utensils },
    ] : [{ path: '/tutores', title: 'Consultar tutores', text: 'Contatos dos responsáveis', icon: Users }]),
    { path: '/consultar-qr', title: 'Consultar QR Code', text: 'Acesse uma internação pelo código', icon: QrCode },
  ]
  return (
    <Page title="Visão geral da clínica" description={`Olá, ${user?.name.split(' ')[0] ?? ''}. Acompanhe os pacientes e organize os cuidados de hoje.`}
      action={<div className="dashboard-heading-actions">
        <button onClick={resource.reload} disabled={resource.loading} aria-label="Atualizar painel" title="Atualizar painel"><RefreshCw size={17} /></button>
        <Link className="clinic-button clinic-primary" to="/internacoes?nova=1"><Plus size={18} />Abrir internação</Link>
      </div>}>
      <div className="dashboard-date"><CalendarDays size={16} />{new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })}<span>Rotina da clínica</span></div>
      <ResourceState {...resource} />
      {data && <>
        <div className="clinic-stats dashboard-stats">
          {[
            { label: 'Internações ativas', value: data.admissions.length, icon: HeartPulse, path: '/internacoes', detail: 'Acompanhar pacientes', tone: 'green' },
            { label: 'Baias disponíveis', value: data.availableBays, icon: House, path: '/baias', detail: 'Consultar ocupação', tone: 'blue' },
            { label: 'Pacientes cadastrados', value: data.patients, icon: PawPrint, path: '/pacientes', detail: 'Abrir cadastros', tone: 'violet' },
            { label: 'Tutores cadastrados', value: data.tutors, icon: Users, path: '/tutores', detail: 'Consultar responsáveis', tone: 'amber' },
          ].map(item => <Link key={item.path} className={`clinic-stat dashboard-stat tone-${item.tone}`} to={item.path}>
            <span className="dashboard-stat-top"><small>{item.label}</small><span className="dashboard-stat-icon"><item.icon size={21} /></span></span>
            <strong>{item.value}</strong><span className="dashboard-stat-bottom">{item.detail}<ArrowRight size={15} /></span>
          </Link>)}
        </div>
        {clinical && <FeedingSummary />}
        <div className="dashboard-workspace">
          <section className="clinic-panel dashboard-patients" aria-labelledby="dashboard-patients-title">
            <div className="clinic-section-heading">
              <div><span className="dashboard-section-label">ACOMPANHAMENTO</span><h2 id="dashboard-patients-title">Pacientes internados <span className="dashboard-count">{data.admissions.length}</span></h2><p>Abra a internação para consultar e registrar os cuidados.</p></div>
              <Link to="/internacoes">Ver todas <ArrowRight size={14} /></Link>
            </div>
            <div className="dashboard-patient-tools">
              <label className="clinic-search"><span className="visually-hidden">Buscar paciente internado</span><span className="clinic-search-input"><Search size={17} /><input type="search" placeholder="Buscar paciente, baia ou veterinário" value={search} onChange={e => setSearch(e.target.value)} /></span></label>
              {user?.role === 'VETERINARIO' && <button className={onlyMine ? 'selected' : ''} aria-pressed={onlyMine} onClick={() => setOnlyMine(!onlyMine)}>Meus pacientes</button>}
            </div>
            {admissions.length ? <>
              <div className="dashboard-list-labels" aria-hidden="true"><span>Paciente / baia</span><span>Responsável</span><span /></div>
              {admissions.slice(0, 6).map(i => <Link className="dashboard-patient-row" key={i.id} to={'/internacoes/' + i.id}>
                <span className="dashboard-patient-identity"><PatientAvatar patient={{ id: i.pacienteId, nome: i.pacienteNome, fotoVersao: i.fotoVersao ?? null }} photoPath={'/internacoes/' + i.id + '/foto'} compact /><span><strong>{i.pacienteNome}</strong><small>Baia {i.baiaIdentificacao} · <StatusBadge status={i.status} /></small></span></span>
                <span className="dashboard-patient-vet"><strong>{i.veterinarioNome}</strong><small>Entrada {dateTime(i.entradaInternacao)}</small></span>
                <ArrowRight size={18} />
              </Link>)}
              {admissions.length > 6 && <p className="dashboard-list-footer">Exibindo 6 de {admissions.length} pacientes. <Link to="/internacoes">Consultar todas as internações</Link></p>}
            </> : <EmptyState title={data.admissions.length ? 'Nenhum paciente nesta busca' : 'Nenhum paciente internado'}><p>{data.admissions.length ? 'Tente outro nome, baia ou veterinário.' : 'As novas internações aparecerão aqui para acompanhamento.'}</p></EmptyState>}
          </section>
          <aside className="dashboard-aside">
            <section className="clinic-panel dashboard-shortcuts"><span className="dashboard-section-label">NO DIA A DIA</span><h2>Acesso rápido</h2>
              {shortcuts.map(item => <Link key={item.path} to={item.path}><span className="dashboard-shortcut-icon"><item.icon size={20} /></span><span><strong>{item.title}</strong><small>{item.text}</small></span><ArrowRight size={16} /></Link>)}
            </section>
            <section className="clinic-panel dashboard-bays"><div className="clinic-section-heading"><div><h2>Ocupação das baias</h2><p>Disponibilidade dos espaços</p></div><House size={20} /></div>
              {data.bays.length ? <>
                <div className="dashboard-occupancy-number"><strong>{data.bays.filter(b => b.status === 'OCUPADA').length}<small> / {data.bays.length}</small></strong><span>ocupadas</span></div>
                <div className="dashboard-occupancy-bar" aria-hidden="true">{(['OCUPADA', 'DISPONIVEL', 'MANUTENCAO'] as const).map(status => <span key={status} className={'bay-' + status.toLowerCase()} style={{ width: `${data.bays.filter(b => b.status === status).length / data.bays.length * 100}%` }} />)}</div>
                <div className="dashboard-bay-legend">{(['OCUPADA', 'DISPONIVEL', 'MANUTENCAO'] as const).map(status => <span key={status}><i className={'bay-' + status.toLowerCase()} /><StatusBadge status={status} /> {data.bays.filter(b => b.status === status).length}</span>)}</div>
              </> : <p>Nenhuma baia cadastrada.</p>}
              <Link className="dashboard-bays-link" to="/baias">Consultar baias <ArrowRight size={15} /></Link>
            </section>
          </aside>
        </div>
      </>}
    </Page>
  )
}
