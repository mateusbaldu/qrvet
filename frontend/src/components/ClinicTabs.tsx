import { useId, type KeyboardEvent, type ReactNode } from 'react'
import '../styles/clinic-navigation.css'

export function ClinicTabs({ tabs, value, onChange, label }: {
  tabs: { id: string; label: string; content: ReactNode }[]
  value: string
  onChange: (value: string) => void
  label: string
}) {
  const id = useId()
  function navigate(event: KeyboardEvent<HTMLButtonElement>, index: number) {
    let next = index
    if (event.key === 'ArrowRight') next = (index + 1) % tabs.length
    else if (event.key === 'ArrowLeft') next = (index + tabs.length - 1) % tabs.length
    else if (event.key === 'Home') next = 0
    else if (event.key === 'End') next = tabs.length - 1
    else return
    event.preventDefault()
    onChange(tabs[next].id)
    document.getElementById(`${id}-tab-${tabs[next].id}`)?.focus()
  }
  return <div className="clinic-tabs">
    <div role="tablist" aria-label={label} className="clinic-tablist">{tabs.map((tab, index) => <button key={tab.id} type="button" role="tab"
      id={`${id}-tab-${tab.id}`} aria-controls={`${id}-panel-${tab.id}`} aria-selected={value === tab.id} tabIndex={value === tab.id ? 0 : -1}
      onClick={() => onChange(tab.id)} onKeyDown={event => navigate(event, index)}>{tab.label}</button>)}</div>
    {tabs.map(tab => <div key={tab.id} role="tabpanel" id={`${id}-panel-${tab.id}`} aria-labelledby={`${id}-tab-${tab.id}`} hidden={value !== tab.id} tabIndex={0}>{tab.content}</div>)}
  </div>
}
