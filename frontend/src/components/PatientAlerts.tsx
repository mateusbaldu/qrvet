import { useState } from 'react'
import { ShieldAlert } from 'lucide-react'
import { useAuth } from '../auth/useAuth'
import { clinicApi, type AlertasPaciente } from '../services/clinic'
import { Editor, Notice } from './ClinicUI'
import '../styles/clinical-records.css'

const fields = [
  { name: 'alergias', label: 'Alergias', hint: 'Informe as alergias conhecidas e as orientações da equipe.' },
  { name: 'restricoesAlimentares', label: 'Restrições alimentares', hint: 'Alimentos ou ingredientes que devem ser evitados.' },
  { name: 'cuidadosManejo', label: 'Cuidados de manejo', hint: 'Cuidados ao manipular, transportar ou acomodar o paciente.' },
] as const

export function PatientAlerts({ alerts, patientId, onSaved }: {
  alerts?: AlertasPaciente; patientId?: number; onSaved?: () => void
}) {
  const { user } = useAuth()
  const [editing, setEditing] = useState(false)
  const [success, setSuccess] = useState('')
  const editable = patientId !== undefined && (user?.role === 'ADMIN' || user?.role === 'VETERINARIO')
  const active = fields.filter(field => alerts?.[field.name]?.trim())
  if (!active.length && !patientId) return null
  return <section className={'clinic-panel patient-alerts' + (active.length ? ' has-alerts' : '')} aria-label="Alertas do paciente">
    <div className="clinic-section-heading">
      <div><h2><ShieldAlert size={20} aria-hidden="true" /> Alertas do paciente</h2><p>{active.length ? 'Confira estas orientações antes de realizar os cuidados.' : 'Nenhum alerta registrado. Confirme as informações com a equipe.'}</p></div>
      {editable && <button onClick={() => { setSuccess(''); setEditing(true) }}>Editar alertas</button>}
    </div>
    {success && <Notice>{success}</Notice>}
    {active.length > 0 && <dl className="patient-alerts-list">{active.map(field => <div key={field.name}><dt>{field.label}</dt><dd>{alerts?.[field.name]}</dd></div>)}</dl>}
    {editing && <Editor title="Editar alertas do paciente" description="Os alertas ficam visíveis à equipe na ficha e nos cuidados. Para remover um alerta, deixe o campo vazio."
      fields={fields.map(field => ({ ...field, type: 'textarea', required: false, maxLength: 1000, value: alerts?.[field.name] ?? '' }))}
      onCancel={() => setEditing(false)} onSave={async data => {
        await clinicApi.save('/pacientes/' + patientId + '/alertas', data, 'PUT')
        setSuccess('Alertas atualizados.')
        onSaved?.()
      }} />}
  </section>
}
