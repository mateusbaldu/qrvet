import type { ReactNode } from 'react'
import { PawPrint } from 'lucide-react'

export function LoginCard({ children }: { children: ReactNode }) {
  return (
    <main className="container-fluid auth-shell p-0">
      <div className="row g-0 min-vh-100">
        <aside className="col-lg-4 d-none d-lg-flex flex-column auth-story">
          <header className="d-flex align-items-center justify-content-between">
            <a href="/" className="auth-brand" aria-label="QRVet - início">
              <span className="brand-symbol"><PawPrint size={27} strokeWidth={2.2} /></span>
              <span className="brand-text">
                <span className="brand-name">QRVet</span>
                <span className="brand-subtitle">Gestão de internação veterinária</span>
              </span>
            </a>
          </header>

          <section className="auth-story-copy">
            <span className="eyebrow">FEITO PARA QUEM CUIDA</span>
            <h1>O cuidado começa<br />com uma equipe<br /><em>conectada.</em></h1>
          </section>

          <div className="auth-art" aria-hidden="true">
            <span className="art-dot" />
            <span className="art-orbit art-orbit-one" />
            <span className="art-orbit art-orbit-two" />
            <span className="art-paw"><PawPrint size={70} strokeWidth={1.5} /></span>
          </div>
        </aside>

        <section className="col-12 col-lg-8 auth-form-side d-flex flex-column">
          <a href="/" className="auth-mobile-brand d-lg-none">
            <span className="brand-symbol"><PawPrint size={23} /></span>QRVet
          </a>
          <div className="auth-form-content my-auto">
            {children}
          </div>
        </section>
      </div>
    </main>
  )
}
