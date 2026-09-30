import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useResource } from '../hooks/useResource'
import { ResourceState } from '../components/ClinicUI'
import { request } from '../services/api'
import { agendaPath, localDate, type AgendaAlimentacao } from '../services/clinic'

export function FeedingSummary() {
  const [now, setNow] = useState(Date.now)
  const date = localDate(new Date(now))
  const resource = useResource(() => request<AgendaAlimentacao[]>(agendaPath(date)), date)
  const reload = resource.reload
  useEffect(() => {
    const timer = setInterval(() => {
      setNow(Date.now())
      reload()
    }, 60000)
    return () => clearInterval(timer)
  }, [reload])
  const pending = resource.data?.filter(a => a.status === 'PENDENTE') ?? []
  return (
    <section className="clinic-panel clinic-feeding-summary">
      <div>
        <h2>Alimentação de hoje</h2>
        <ResourceState {...resource} />
        {resource.data && (
          <p>
            {pending.length} refeições pendentes ·{' '}
            {pending.filter(a => !a.jejumAtivo && Date.parse(a.horario) <= now).length} atrasadas ·{' '}
            {resource.data.filter(a => a.status === 'CONCLUIDA').length} concluídas
          </p>
        )}
      </div>
      <Link className="clinic-button clinic-primary" to="/alimentacao">
        Abrir checklist geral →
      </Link>
    </section>
  )
}
