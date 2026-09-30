import { useEffect, useId, useRef, useState, type ReactNode, type FormEvent } from 'react'
import { CheckCircle2, Inbox, LoaderCircle, X } from 'lucide-react'
import { ApiError, readableError } from '../services/api'
import '../styles/clinic.css'

export function Page({ title, description, children, action }: { title: string; description: string; children: ReactNode; action?: ReactNode }) {
  return <section className="clinic-page"><header className="clinic-heading"><div><p className="clinic-eyebrow">CLÍNICA VETERINÁRIA</p><h1>{title}</h1><p>{description}</p></div>{action}</header>{children}</section>
}
export function ResourceState({ loading, error, reload }: { loading: boolean; error: string; reload: () => void }) {
  return <>{loading && <div className="clinic-state" role="status"><LoaderCircle className="clinic-spinner" size={22} /> Carregando informações…</div>}{error && <div className="clinic-error" role="alert">{error} <button onClick={reload}>Tentar novamente</button></div>}</>
}
export function EmptyState({ title = 'Nenhum registro encontrado', children }: { title?: string; children?: ReactNode }) {
  return <div className="clinic-empty"><span className="clinic-empty-icon"><Inbox size={25} /></span><h2>{title}</h2>{children && <div>{children}</div>}</div>
}
export function Notice({ children }: { children: ReactNode }) {
  return <div className="clinic-success" role="status"><CheckCircle2 size={18} />{children}</div>
}
export function StatusBadge({ status, children }: { status: string; children?: ReactNode }) {
  const text: Record<string, string> = { ATIVA: 'Internado', ALTA: 'Alta', OBITO: 'Óbito', DISPONIVEL: 'Disponível', OCUPADA: 'Ocupada', MANUTENCAO: 'Manutenção' }
  return <span className={'clinic-badge status-' + status.toLowerCase()}>{children ?? text[status] ?? status}</span>
}
export function Modal({ title, children, onClose, busy = false }: { title: string; children: ReactNode; onClose: () => void; busy?: boolean }) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  useEffect(() => {
    const dialog = ref.current!
    const previous = document.activeElement as HTMLElement | null
    dialog.showModal()
    const overflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { dialog.close(); document.body.style.overflow = overflow; previous?.focus() }
  }, [])
  return <dialog className="clinic-modal" ref={ref} aria-labelledby={titleId} onCancel={event => { event.preventDefault(); if (!busy) onClose() }} onClick={event => {
    if (event.target === event.currentTarget && !busy) {
      const box = event.currentTarget.getBoundingClientRect()
      if (event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom) onClose()
    }
  }}>
    <header className="clinic-modal-header"><div><p className="clinic-eyebrow">QRVET · CUIDADO EM CADA ETAPA</p><h2 id={titleId}>{title}</h2></div><button className="clinic-icon-button" type="button" onClick={onClose} disabled={busy} aria-label="Fechar"><X size={20} /></button></header>{children}
  </dialog>
}
export type Field = { name: string; label: string; type?: string; required?: boolean; maxLength?: number; minLength?: number; min?: string; max?: string; step?: string; options?: { value: string | number; label: string }[]; value?: string | number; hint?: string; wide?: boolean; autoComplete?: string }
export function Editor({ title, fields, onSave, onCancel, submitLabel = 'Salvar', description, inline = false, closeOnSave = true }: { title: string; fields: Field[]; onSave: (data: Record<string, string | number>) => Promise<unknown>; onCancel: () => void; submitLabel?: string; description?: ReactNode; inline?: boolean; closeOnSave?: boolean }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const lock = useRef(false)
  const id = useId()
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (lock.current) return
    const form = new FormData(event.currentTarget)
    const data = Object.fromEntries(fields.map(field => {
      const raw = String(form.get(field.name) ?? '')
      return [field.name, field.type === 'number' ? Number(raw) : field.type === 'password' ? raw : raw.trim()]
    }))
    const blank = fields.find(field => field.required !== false && field.type !== 'number' && !String(data[field.name]).trim())
    if (blank) { setError('Preencha o campo ' + blank.label.toLowerCase() + '.'); return }
    lock.current = true; setBusy(true); setError(''); setFieldErrors({})
    try { await onSave(data); if (closeOnSave) onCancel() } catch (error) { setError(readableError(error)); if (error instanceof ApiError) setFieldErrors(error.fields) } finally { lock.current = false; setBusy(false) }
  }
  const content = <>{description && <div className="clinic-form-description">{description}</div>}<form onSubmit={submit}><fieldset disabled={busy}><div className="clinic-fields">{fields.map(field => <label className={field.wide || field.type === 'textarea' ? 'clinic-field-wide' : ''} key={field.name} htmlFor={id + '-' + field.name}>
    <span>{field.label}{field.required !== false && <span aria-hidden="true"> *</span>}</span>
    {field.options ? <select id={id + '-' + field.name} name={field.name} defaultValue={field.value ?? ''} required={field.required !== false} aria-invalid={!!fieldErrors[field.name]}><option value="">Selecione</option>{field.options.map(option => <option value={option.value} key={option.value}>{option.label}</option>)}</select> : field.type === 'textarea' ? <textarea id={id + '-' + field.name} name={field.name} defaultValue={field.value} required={field.required !== false} maxLength={field.maxLength ?? 10000} aria-invalid={!!fieldErrors[field.name]} /> : <input id={id + '-' + field.name} name={field.name} type={field.type ?? 'text'} defaultValue={field.value} required={field.required !== false} maxLength={field.maxLength} minLength={field.minLength} min={field.min} max={field.max} step={field.step} autoComplete={field.autoComplete} aria-invalid={!!fieldErrors[field.name]} />}
    {field.hint && <small>{field.hint}</small>}{fieldErrors[field.name] && <small className="clinic-field-error">{fieldErrors[field.name]}</small>}
  </label>)}</div>{error && <p className="clinic-error" role="alert">{error}</p>}<div className="clinic-form-actions"><button type="button" onClick={onCancel}>Cancelar</button><button className="clinic-primary" type="submit">{busy ? 'Salvando…' : submitLabel}</button></div></fieldset></form></>
  return inline ? <section className="clinic-panel clinic-editor"><h2>{title}</h2>{content}</section> : <Modal title={title} onClose={onCancel} busy={busy}>{content}</Modal>
}
export function Pagination({ page, pages, total, onChange }: { page: number; pages: number; total: number; onChange: (page: number) => void }) {
  return <footer className="clinic-pagination"><span>{total} registro{total === 1 ? '' : 's'} · Página {page + 1} de {Math.max(1, pages)}</span><div><button disabled={page === 0} onClick={() => onChange(page - 1)}>Anterior</button><button disabled={page + 1 >= pages} onClick={() => onChange(page + 1)}>Próxima</button></div></footer>
}

