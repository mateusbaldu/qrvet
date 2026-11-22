import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, Utensils } from 'lucide-react'
import { useResource } from '../hooks/useResource'
import { ResourceState } from '../components/ClinicUI'
import { request } from '../services/api'
import { agendaPath, localDate, type AgendaAlimentacao } from '../services/clinic'

export function FeedingSummary() {
  const [now, setNow] = useState(Date.now)
  const date = localDate(new Date(now))
  const resource = useResource(() => request<AgendaAlimentacao[]>(agendaPath(date)), [date])
  const reload = resource.reload
  useEffect(() => {
    const timer = setInterval(() => { setNow(Date.now()); reload() }, 60000)
    return () => clearInterval(timer)
  }, [reload])
  const pending = resource.data?.filter(a => a.status === 'PENDENTE') ?? []
  const late = pending.filter(a => !a.jejumAtivo && Date.parse(a.horario) <= now)
  const completed = resource.data?.filter(a => a.status === 'CONCLUIDA').length ?? 0
  const fasting = pending.filter(a => a.jejumAtivo).length
  const next = pending.filter(a => !a.jejumAtivo).sort((a, b) => Date.parse(a.horario) - Date.parse(b.horario)).slice(0, 3)
  return (
    <section className="clinic-panel dashboard-feeding" aria-labelledby="dashboard-feeding-title">
      <div className="clinic-section-heading">
        <h2 id="dashboard-feeding-title"><Utensils size={19} aria-hidden="true" /> Alimentação de hoje</h2>
        <Link to="/alimentacao">Abrir checklist <ArrowRight size={15} /></Link>
      </div>
      <ResourceState {...resource} />
      {resource.data && <>
        <div className="dashboard-feeding-body">
          <div className="dashboard-feeding-metrics">
            {[
              { label: 'Pendentes', filter: 'PENDENTE', value: pending.length, tone: 'pending' },
              { label: 'Atrasadas', filter: 'ATRASADA', value: late.length, tone: late.length ? 'late' : 'pending' },
              { label: 'Concluídas', filter: 'CONCLUIDA', value: completed, tone: 'done' },
            ].map(item => <Link to={'/alimentacao?filtro=' + item.filter} key={item.label} className={'dashboard-feeding-metric ' + item.tone}><strong>{item.value}</strong><span>{item.label}</span></Link>)}
          </div>
          <div className="dashboard-next-meals"><span className="dashboard-next-title">{late.length ? 'Refeições aguardando registro' : 'Próximas refeições'}</span>
            {next.length ? next.map(item => <Link to={'/cuidados?internacao=' + item.internacaoId} key={item.id}><time dateTime={item.horario}>{new Date(item.horario).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}</time><span>{item.pacienteNome}<small>Baia {item.baiaIdentificacao}</small></span><span className={'dashboard-meal-state ' + (Date.parse(item.horario) <= now ? 'late' : '')}>{Date.parse(item.horario) <= now ? 'Atrasada' : 'Pendente'}</span></Link>) : <p>{fasting ? 'As refeições pendentes estão bloqueadas por jejum.' : 'Nenhuma refeição pendente para hoje.'}</p>}
          </div>
        </div>
        <p className="dashboard-feeding-note">Atualização automática a cada minuto.{fasting > 0 && ` ${fasting} refeição(ões) pendente(s) com jejum ativo.`}</p>
      </>}
    </section>
  )
}
