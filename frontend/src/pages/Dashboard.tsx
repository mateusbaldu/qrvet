import { Link } from 'react-router-dom'
import { ArrowRight, HeartPulse, House, PawPrint, Plus, QrCode, Users } from 'lucide-react'
import { Page, ResourceState, EmptyState, StatusBadge } from '../components/ClinicUI'
import { useResource } from '../hooks/useResource'
import { allPages, clinicApi, dateTime, type Baia, type Internacao } from '../services/clinic'

export function Dashboard() {
  const resource = useResource(async () => {
    const [patients, tutors, bays, admissions] = await Promise.all([
      clinicApi.list('/pacientes'), clinicApi.list('/tutores'), allPages<Baia>('/baias'), allPages<Internacao>('/internacoes/ativas'),
    ])
    return { patients: patients.total, tutors: tutors.total, bays, admissions }
  }, 'dashboard')
  const data = resource.data
  return <Page title="Um bom dia começa com cuidado." description="Uma visão da clínica para organizar o seu próximo atendimento." action={<Link className="clinic-button clinic-primary" to="/internacoes?nova=1"><Plus size={17} />Abrir internação</Link>}>
    <ResourceState {...resource} />
    {data && <>
      <div className="clinic-stats">
        {[{ label: 'Pacientes cadastrados', value: data.patients, icon: PawPrint, path: '/pacientes' }, { label: 'Internações ativas', value: data.admissions.length, icon: HeartPulse, path: '/internacoes' }, { label: 'Baias disponíveis', value: data.bays.filter(b => b.status === 'DISPONIVEL').length, icon: House, path: '/baias' }, { label: 'Tutores cadastrados', value: data.tutors, icon: Users, path: '/tutores' }].map(item => <Link key={item.path} className="clinic-stat" to={item.path}><span><item.icon size={20} /><ArrowRight size={16} /></span><strong>{item.value}</strong><small>{item.label}</small></Link>)}
      </div>
      <div className="clinic-detail-grid"><section className="clinic-panel"><div className="clinic-section-heading"><div><h2>Quem está sob nossos cuidados</h2><p>As internações mais recentes da clínica.</p></div><Link to="/internacoes">Ver todas →</Link></div>
        {data.admissions.length ? data.admissions.slice(0, 6).map(i => <Link className="clinic-dashboard-row" key={i.id} to={'/internacoes/' + i.id}><span className="clinic-avatar"><PawPrint size={19} /></span><span><strong>{i.pacienteNome}</strong><small>Baia {i.baiaIdentificacao} · Entrada {dateTime(i.entradaInternacao)}</small></span><StatusBadge status={i.status} /><ArrowRight size={17} /></Link>) : <EmptyState title="Tudo tranquilo por aqui"><p>Nenhum paciente internado no momento.</p></EmptyState>}
      </section><aside><section className="clinic-panel clinic-shortcuts"><h2>Próximos passos</h2><Link to="/pacientes"><PawPrint size={20} /><span><strong>Pacientes</strong><small>Consultar e cadastrar animais</small></span><ArrowRight size={16} /></Link><Link to="/tutores"><Users size={20} /><span><strong>Tutores</strong><small>Dados dos responsáveis</small></span><ArrowRight size={16} /></Link><Link to="/consultar-qr"><QrCode size={20} /><span><strong>Consultar QR Code</strong><small>Abrir o acompanhamento</small></span><ArrowRight size={16} /></Link></section><section className="clinic-panel"><h2>Ocupação das baias</h2><div className="clinic-occupancy" aria-label="Ocupação das baias">{data.bays.map(b => <Link title={b.identificacao + ' · ' + b.status} to="/baias" key={b.id}><StatusBadge status={b.status}>{b.identificacao}</StatusBadge></Link>)}</div>{!data.bays.length && <p>Nenhuma baia cadastrada.</p>}<p className="clinic-muted">{data.bays.filter(b => b.status === 'OCUPADA').length} ocupadas de {data.bays.length} baias</p></section></aside></div>
    </>}
  </Page>
}
