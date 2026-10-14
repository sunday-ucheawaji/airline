import type { ReactNode } from 'react'

type AuthLayoutProps = {
  title: string
  /** The document title; the heading is reused when omitted. */
  pageTitle?: string
  intro?: ReactNode
  children: ReactNode
  aside?: ReactNode
}

export function AuthLayout({ title, pageTitle, intro, children, aside }: AuthLayoutProps) {
  return (
    <section
      aria-labelledby="auth-title"
      className="mx-auto grid max-w-6xl gap-x-16 gap-y-10 px-5 pt-8 pb-20 sm:px-8 sm:pt-14 lg:grid-cols-[minmax(0,27rem)_minmax(0,1fr)]"
    >
      <title>{`${pageTitle ?? title} | Contrail`}</title>
      <div>
        <h1 id="auth-title" className="font-display text-[clamp(2.5rem,6vw,3.75rem)]">
          {title}
        </h1>
        {intro && <div className="mt-4 max-w-[46ch] text-muted-foreground">{intro}</div>}
        <div className="mt-8">{children}</div>
      </div>
      {aside && <div className="hidden lg:block">{aside}</div>}
    </section>
  )
}

/** The Contrail mark drawn large: two vapour trails crossing the panel, the account's reasons sitting beneath them. */
export function AccountAside() {
  return (
    <aside aria-label="Why sign in" className="relative h-full min-h-[28rem] overflow-hidden rounded-lg bg-card p-10 shadow-[0_0_0_1px_var(--rule)]">
      <svg aria-hidden viewBox="0 0 400 260" className="auth-trails absolute inset-x-0 top-0 h-auto w-full text-chart-blue">
        <path d="M-20 250 L380 20" stroke="currentColor" strokeWidth="5" strokeLinecap="round" pathLength="1" />
        <path d="M-20 200 L300 12" stroke="currentColor" strokeWidth="5" strokeLinecap="round" opacity="0.4" pathLength="1" />
        <circle cx="388" cy="15" r="7" fill="currentColor" />
      </svg>
      <div className="relative mt-48 grid gap-6">
        <p className="font-display text-4xl">Your trips, kept together.</p>
        <ul className="grid gap-3 text-muted-foreground">
          <li>See every booking you’ve made in one list.</li>
          <li>Add bags, meals or seats to a trip you’ve already booked.</li>
          <li>Change or cancel within your fare’s rules.</li>
        </ul>
        <p className="text-sm text-muted-foreground">Searching for flights never needs an account.</p>
      </div>
    </aside>
  )
}
