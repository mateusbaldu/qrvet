import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, CalendarDays, Fence, HeartPulse, PawPrint, Plus, RefreshCw, Search, Users } from 'lucide-react'
import { useAuth } from '../auth/useAuth'
import { Page, ResourceState, EmptyState, StatusBadge } from '../components/ClinicUI'
import { useResource } from '../hooks/useResource'
import { allPages, clinicApi, dateTime, searchText, type Baia, type Internacao } from '../services/clinic'
import { FeedingSummary } from './FeedingSummary'
import { PatientAvatar } from '../components/PatientEditor'
import '../styles/dashboard.css'

const bayStatuses = ['OCUPADA', 'DISPONIVEL', 'MANUTENCAO'] as const

export function Dashboard() {
  const { user } = useAuth()
  const [search, setSearch] = useState('')
  const [onlyMine, setOnlyMine] = useState(false)
  const resource = useResource(async () => {
    const [patients, tutors, bays, admissions] = await Promise.all([
      clinicApi.list('/pacientes'), clinicApi.list('/tutores'),
      allPages<Baia>('/baias'), allPages<Internacao>('/internacoes/ativas'),
    ])
    const bayCounts = { OCUPADA: 0, DISPONIVEL: 0, MANUTENCAO: 0 }
    for (const bay of bays) bayCounts[bay.status]++
    return { patients: patients.total, tutors: tutors.total, bayTotal: bays.length, bayCounts,
      admissions: admissions.sort((a, b) => Date.parse(b.entradaInternacao) - Date.parse(a.entradaInternacao)),
    }
  }, ['dashboard'])
  const data = resource.data
  const clinical = user?.role === 'ADMIN' || user?.role === 'VETERINARIO'
  const admissions = data?.admissions.filter(i =>
    (!onlyMine || i.veterinarioId === user?.id) &&
    searchText([i.pacienteNome, i.baiaIdentificacao, i.veterinarioNome].join(' ')).includes(searchText(search.trim()))
  ) ?? []
  return (
    <Page title="Visão geral da clínica" description={`Olá, ${user?.name.split(' ')[0] ?? ''}. Acompanhe os pacientes e organize os cuidados de hoje.`}
      action={<div className="dashboard-heading-actions">
        <button onClick={resource.reload} disabled={resource.loading} aria-label="Atualizar painel" title="Atualizar painel"><RefreshCw size={17} /></button>
        <Link className="clinic-button clinic-primary" to="/internacoes?nova=1"><Plus size={18} />Abrir internação</Link>
      </div>}>
      <div className="dashboard-date"><CalendarDays size={16} />{new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })}</div>
      <ResourceState {...resource} />
      {data && <>
        <div className="clinic-stats dashboard-stats">
          {[
            { label: 'Internações ativas', value: data.admissions.length, icon: HeartPulse, path: '/internacoes', detail: 'Acompanhar pacientes', tone: 'green' },
            { label: 'Baias disponíveis', value: data.bayCounts.DISPONIVEL, icon: Fence, path: '/baias', detail: 'Consultar ocupação', tone: 'blue' },
            { label: 'Pacientes cadastrados', value: data.patients, icon: PawPrint, path: '/pacientes', detail: 'Abrir cadastros', tone: 'violet' },
            { label: 'Tutores cadastrados', value: data.tutors, icon: Users, path: '/tutores', detail: 'Consultar responsáveis', tone: 'amber' },
          ].map(item => <Link key={item.path} className={`clinic-stat dashboard-stat tone-${item.tone}`} to={item.path}>
            <span className="dashboard-stat-top"><small>{item.label}</small><span className="dashboard-stat-icon"><item.icon size={21} /></span></span>
            <strong>{item.value}</strong><span className="dashboard-stat-bottom">{item.detail}<ArrowRight size={15} /></span>
          </Link>)}
        </div>
        <div className={clinical ? 'dashboard-summary dashboard-summary-paired' : 'dashboard-summary'}>
          {clinical && <FeedingSummary />}
          <section className="clinic-panel dashboard-bays" aria-labelledby="dashboard-bays-title">
            <h2 id="dashboard-bays-title"><Fence size={20} aria-hidden="true" /> Ocupação das baias</h2>
            {data.bayTotal ? <>
              <div className="dashboard-occupancy-number"><strong>{data.bayCounts.OCUPADA}<small> / {data.bayTotal}</small></strong><span>ocupadas</span></div>
              <div className="dashboard-occupancy-bar" aria-hidden="true">
                {bayStatuses.map(status => <span key={status} className={'bay-' + status.toLowerCase()} style={{ width: `${data.bayCounts[status] / data.bayTotal * 100}%` }} />)}
              </div>
              <div className="dashboard-bay-legend">
                {bayStatuses.map(status => <span key={status}><i className={'bay-' + status.toLowerCase()} aria-hidden="true" /><StatusBadge status={status} /> {data.bayCounts[status]}</span>)}
              </div>
            </> : <p>Nenhuma baia cadastrada.</p>}
            <Link className="dashboard-bays-link" to="/baias">Consultar baias <ArrowRight size={15} /></Link>
          </section>
        </div>
        <section className="clinic-panel dashboard-patients" aria-labelledby="dashboard-patients-title">
          <div className="clinic-section-heading">
            <h2 id="dashboard-patients-title">Pacientes internados <span className="dashboard-count">{data.admissions.length}</span></h2>
            <Link to="/internacoes">Ver todas <ArrowRight size={14} /></Link>
          </div>
          <div className="dashboard-patient-tools">
            <label className="clinic-search"><span className="visually-hidden">Buscar paciente internado</span><span className="clinic-search-input"><Search size={17} /><input type="search" placeholder="Buscar paciente, baia ou veterinário" value={search} onChange={e => setSearch(e.target.value)} /></span></label>
            {user?.role === 'VETERINARIO' && <button className={onlyMine ? 'selected' : ''} aria-pressed={onlyMine} onClick={() => setOnlyMine(!onlyMine)}>Meus pacientes</button>}
          </div>
          {admissions.length ? <>
            <div className="dashboard-list-labels" aria-hidden="true"><span>Paciente / baia</span><span>Veterinário</span><span>Entrada</span><span>Motivo</span></div>
            {admissions.slice(0, 6).map(i => <Link className="dashboard-patient-row" key={i.id} to={'/internacoes/' + i.id}>
              <span className="dashboard-patient-identity"><PatientAvatar patient={{ id: i.pacienteId, nome: i.pacienteNome, fotoVersao: i.fotoVersao ?? null }} photoPath={'/internacoes/' + i.id + '/foto'} compact /><span><strong>{i.pacienteNome}</strong><small>Baia {i.baiaIdentificacao} · <StatusBadge status={i.status} /></small></span></span>
              <span className="dashboard-patient-detail"><span className="dashboard-patient-label">Veterinário</span>{i.veterinarioNome}</span>
              <span className="dashboard-patient-detail"><span className="dashboard-patient-label">Entrada</span><time dateTime={i.entradaInternacao}>{dateTime(i.entradaInternacao)}</time></span>
              <span className="dashboard-patient-detail dashboard-patient-reason">
                <span className="dashboard-patient-label">Motivo</span>
                <span className="dashboard-patient-reason-text">{i.motivo?.trim() || '—'}</span>
              </span>
              <ArrowRight size={18} aria-hidden="true" />
            </Link>)}
            {admissions.length > 6 && <p className="dashboard-list-footer">Exibindo 6 de {admissions.length} pacientes. <Link to="/internacoes">Consultar todas as internações</Link></p>}
          </> : <EmptyState title={data.admissions.length ? 'Nenhum paciente nesta busca' : 'Nenhum paciente internado'}><p>{data.admissions.length ? 'Tente outro nome, baia ou veterinário.' : 'As novas internações aparecerão aqui para acompanhamento.'}</p></EmptyState>}
        </section>
      </>}
    </Page>
  )
}
