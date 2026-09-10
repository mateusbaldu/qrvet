import { useEffect, useRef, useState, type ReactNode, type FormEvent } from 'react'
import { readableError } from '../services/api'
import '../styles/clinic.css'

export function Page({ title, description, children, action }: { title: string; description: string; children: ReactNode; action?: ReactNode }) {
  return <section className="clinic-page"><header className="clinic-heading"><div><p className="clinic-eyebrow">CUIDADO EM CADA ETAPA</p><h1>{title}</h1><p>{description}</p></div>{action}</header>{children}</section>
}
export function ResourceState({ loading, error, reload }: { loading: boolean; error: string; reload: () => void }) {
  return <>{loading && <p className="clinic-state" role="status">Carregando…</p>}{error && <div className="clinic-error" role="alert">{error} <button onClick={reload}>Tentar novamente</button></div>}</>
}
export type Field = { name: string; label: string; type?: string; required?: boolean; maxLength?: number; minLength?: number; min?: string; max?: string; step?: string; options?: { value: string | number; label: string }[]; value?: string | number; hint?: string }
export function Editor({ title, fields, onSave, onCancel, submitLabel = 'Salvar' }: { title: string; fields: Field[]; onSave: (data: Record<string, string | number>) => Promise<unknown>; onCancel: () => void; submitLabel?: string }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const lock = useRef(false)
  const first = useRef<HTMLHeadingElement>(null)
  useEffect(() => { first.current?.focus() }, [])
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (lock.current) return
    const form = new FormData(e.currentTarget)
    const data = Object.fromEntries(fields.map(f => [f.name, f.type === 'number' ? Number(form.get(f.name)) : String(form.get(f.name) ?? '').trim()]))
    lock.current = true; setBusy(true); setError('')
    try { await onSave(data); onCancel() } catch (e) { setError(readableError(e)) } finally { lock.current = false; setBusy(false) }
  }
  return <section className="clinic-panel clinic-editor"><h2 ref={first} tabIndex={-1}>{title}</h2><form onSubmit={submit}><fieldset disabled={busy}><div className="clinic-fields">{fields.map(f => <label key={f.name}>{f.label}{f.required !== false && ' *'}{f.options ? <select name={f.name} defaultValue={f.value ?? ''} required={f.required !== false}><option value="">Selecione</option>{f.options.map(o => <option value={o.value} key={o.value}>{o.label}</option>)}</select> : f.type === 'textarea' ? <textarea name={f.name} defaultValue={f.value} required={f.required !== false} maxLength={f.maxLength ?? 10000} /> : <input name={f.name} type={f.type ?? 'text'} defaultValue={f.value} required={f.required !== false} maxLength={f.maxLength} minLength={f.minLength} min={f.min} max={f.max} step={f.step} />}{f.hint && <small>{f.hint}</small>}</label>)}</div>{error && <p className="clinic-error" role="alert">{error}</p>}<div className="clinic-actions"><button className="clinic-primary" type="submit">{busy ? 'Salvando…' : submitLabel}</button><button type="button" onClick={onCancel}>Cancelar</button></div></fieldset></form></section>
}
export function Pagination({ page, pages, total, onChange }: { page: number; pages: number; total: number; onChange: (page: number) => void }) {
  return <footer className="clinic-pagination"><span>{total} registro(s) · Página {page + 1} de {Math.max(1, pages)}</span><div><button disabled={page === 0} onClick={() => onChange(page - 1)}>Anterior</button><button disabled={page + 1 >= pages} onClick={() => onChange(page + 1)}>Próxima</button></div></footer>
}


