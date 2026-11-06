import { useEffect, useId, useRef, useState } from 'react'
import { CircleHelp } from 'lucide-react'
import '../styles/onboarding.css'

/** Short contextual help, available with mouse, keyboard and touch. */
export function HelpTip({ label, children }: { label: string; children: string }) {
  const id = useId()
  const root = useRef<HTMLSpanElement>(null)
  const [open, setOpen] = useState(false)
  useEffect(() => {
    if (!open) return
    const dismiss = (event: PointerEvent) => {
      if (!root.current?.contains(event.target as Node)) setOpen(false)
    }
    const escape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('pointerdown', dismiss)
    document.addEventListener('keydown', escape)
    return () => {
      document.removeEventListener('pointerdown', dismiss)
      document.removeEventListener('keydown', escape)
    }
  }, [open])
  return (
    <span className="help-tip" ref={root} onMouseEnter={() => setOpen(true)} onMouseLeave={() => setOpen(false)}>
      <button type="button" className="help-tip-trigger" aria-label={label}
        aria-describedby={open ? id : undefined}
        onFocus={() => setOpen(true)} onBlur={() => setOpen(false)}
        onClick={() => setOpen(true)}>
        <CircleHelp size={18} aria-hidden="true" />
      </button>
      {open && <span className="help-tip-content" role="tooltip" id={id}>{children}</span>}
    </span>
  )
}
