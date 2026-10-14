import { Link, useLocation, useNavigate } from 'react-router'
import { useMyAirlines } from '@/features/airline-ops/api'
import { useLogout, useSession } from '@/lib/api'
import { Wordmark } from './Wordmark'

const navLink = 'rounded-md px-1 py-1 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground'
const accountLink = 'rounded-md text-sm font-semibold text-foreground underline-offset-4 hover:underline'

export function SiteHeader() {
  return (
    <header className="mx-auto flex w-full max-w-6xl items-center justify-between gap-6 px-5 py-5 sm:px-8">
      <Wordmark />
      <nav aria-label="Main" className="flex items-center gap-5 sm:gap-7">
        <a href="/#search" className={`${navLink} hidden sm:inline`}>
          Flights
        </a>
        <a href="/#manage-booking" className={navLink}>
          Manage booking
        </a>
        <a href="/#airlines" className={`${navLink} hidden md:inline`}>
          For airlines
        </a>
        <AccountNav />
      </nav>
    </header>
  )
}

function AccountNav() {
  const { user, isAuthenticated, isLoading } = useSession()
  const logout = useLogout()
  const navigate = useNavigate()
  const { pathname, search } = useLocation()

  if (isLoading) return <span aria-hidden className="w-14" />

  if (isAuthenticated && user) {
    return (
      <div className="flex items-center gap-4">
        <AirlineLink />
        <span className="hidden text-sm text-muted-foreground sm:inline">{user.firstName}</span>
        <button
          type="button"
          className={accountLink}
          disabled={logout.isPending}
          onClick={() => logout.mutate(undefined, { onSettled: () => navigate('/') })}
        >
          {logout.isPending ? 'Signing out…' : 'Sign out'}
        </button>
      </div>
    )
  }

  const onAuthPage = ['/sign-in', '/sign-up'].includes(pathname)
  const redirect = onAuthPage || pathname === '/' ? '' : `?redirect=${encodeURIComponent(pathname + search)}`
  return (
    <div className="flex items-center gap-4">
      <Link to={`/sign-in${redirect}`} className={accountLink}>
        Sign in
      </Link>
      <Link
        to="/sign-up"
        className="hidden rounded-md bg-primary px-3 py-1.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90 sm:inline-block"
      >
        Create account
      </Link>
    </div>
  )
}

/** Only airline staff see the way into their airline's workspace. */
function AirlineLink() {
  const airlines = useMyAirlines()
  if (!airlines.data?.length) return null
  return (
    <Link to="/airline" className={navLink}>
      {airlines.data.length === 1 ? airlines.data[0].name : 'Your airlines'}
    </Link>
  )
}
