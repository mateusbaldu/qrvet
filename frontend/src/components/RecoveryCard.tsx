import type { ReactNode } from 'react'
import { PawPrint, ShieldCheck } from 'lucide-react'
import { Link } from 'react-router-dom'

type RecoveryCardProps = {
  eyebrow: string
  title: string
  description: string
  children: ReactNode
}

export function RecoveryCard({ eyebrow, title, description, children }: RecoveryCardProps) {
  return (
    <main className="recovery-shell">
      <Link to="/login" className="recovery-brand" aria-label="QRVet - voltar ao login">
        <span className="brand-symbol"><PawPrint size={25} strokeWidth={2.2} /></span>
        <span className="brand-text">
          <span className="brand-name">QRVet</span>
          <span className="brand-subtitle">Gestão de internação veterinária</span>
        </span>
      </Link>

      <section className="recovery-card">
        <span className="recovery-eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
        <p className="recovery-description">{description}</p>
        {children}
      </section>

      <p className="recovery-security">
        <ShieldCheck size={15} /> Seu acesso é pessoal.
      </p>
    </main>
  )
}
