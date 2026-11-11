import { useState } from 'react'
import { NotebookPen, Plus } from 'lucide-react'
import { useResource } from '../hooks/useResource'
import { clinicApi, dateTime, type Evolucao } from '../services/clinic'
import { Editor, EmptyState, Notice, Pagination, ResourceState } from './ClinicUI'
import '../styles/clinical-records.css'

export function EvolutionNotes({ admissionId, active }: { admissionId: string; active: boolean }) {
  const [page, setPage] = useState(0)
  const [creating, setCreating] = useState(false)
  const [success, setSuccess] = useState('')
  const resource = useResource(() => clinicApi.list<Evolucao>('/internacoes/' + admissionId + '/evolucoes', page, { size: 5 }), [admissionId, page, active])
  return <section className="clinic-panel evolution-notes" aria-labelledby="evolution-notes-title">
    <div className="clinic-section-heading"><div><h2 id="evolution-notes-title"><NotebookPen size={20} aria-hidden="true" /> Evoluções do paciente</h2><p>Anotações da internação, com data e profissional responsável.</p></div>
      {active && <button className="clinic-primary" onClick={() => setCreating(true)}><Plus size={16} /> Adicionar evolução</button>}
    </div>
    {success && <Notice>{success}</Notice>}
    {!active && <p className="clinic-muted">Internação encerrada. Evoluções disponíveis para consulta.</p>}
    <ResourceState {...resource} />
    {resource.data && <>
      {resource.data.items.length ? <ol className="evolution-notes-list">{resource.data.items.map(note => <li key={note.id}>
        <header><strong>{note.profissionalNome}</strong><time dateTime={note.dataHoraRegistro}>{dateTime(note.dataHoraRegistro)}</time></header><p>{note.texto}</p>
      </li>)}</ol> : <EmptyState title="Nenhuma evolução registrada"><p>As anotações desta internação aparecerão aqui.</p></EmptyState>}
      {resource.data.pages > 1 && <Pagination {...resource.data} onChange={setPage} />}
    </>}
    {creating && <Editor title="Adicionar evolução" description="Registre observações sobre este paciente. O horário e a autoria serão salvos automaticamente. Após salvar, o registro é preservado; correções devem ser feitas em uma nova anotação."
      fields={[{ name: 'texto', label: 'Anotação de evolução', type: 'textarea', maxLength: 10000 }]}
      submitLabel="Salvar evolução" onCancel={() => setCreating(false)} onSave={async data => {
        await clinicApi.save('/internacoes/' + admissionId + '/evolucoes', data)
        setSuccess('Evolução registrada.')
        setPage(0)
        resource.reload()
      }} />}
  </section>
}
