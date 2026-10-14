import { Link } from 'react-router'
import { Wordmark } from './Wordmark'

const currentYear = new Date().getFullYear()

const footerLink = 'rounded-sm text-sm text-muted-foreground hover:text-foreground'

const groups = [
  {
    title: 'Travel',
    links: [
      { label: 'Search flights', to: '/#search' },
      { label: 'Manage booking', to: '/#manage-booking' },
      { label: 'Help', to: '/help' },
    ],
  },
  {
    title: 'Airlines',
    links: [
      { label: 'Apply to join', to: '/airlines/apply' },
      { label: 'Airline sign-in', to: '/sign-in' },
    ],
  },
  {
    title: 'Contrail team',
    links: [{ label: 'Staff sign-in', to: '/staff/sign-in' }],
  },
]

export function SiteFooter() {
  return (
    <footer className="border-t">
      <div className="mx-auto grid max-w-6xl gap-10 px-5 py-12 sm:px-8 md:grid-cols-[1fr_auto]">
        <div className="max-w-xs">
          <Wordmark />
          <p className="mt-3 text-sm text-muted-foreground">Flights from many airlines, booked in one place.</p>
        </div>
        <div className="grid grid-cols-2 gap-8 sm:grid-cols-3 sm:gap-14">
          {groups.map((group) => (
            <nav key={group.title} aria-label={group.title}>
              <h2 className="text-sm font-semibold">{group.title}</h2>
              <ul className="mt-3 grid gap-2">
                {group.links.map((link) => (
                  <li key={link.label}>
                    {link.to.startsWith('/#') ? (
                      <a href={link.to} className={footerLink}>
                        {link.label}
                      </a>
                    ) : (
                      <Link to={link.to} className={footerLink}>
                        {link.label}
                      </Link>
                    )}
                  </li>
                ))}
              </ul>
            </nav>
          ))}
        </div>
      </div>
      <div className="mx-auto max-w-6xl px-5 pb-8 text-sm text-muted-foreground sm:px-8">
        © {currentYear} Contrail
      </div>
    </footer>
  )
}
