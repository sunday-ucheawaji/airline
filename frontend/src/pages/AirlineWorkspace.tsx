import { Link, Navigate, NavLink, Outlet, useParams } from 'react-router'
import { FormAlert } from '@/components/form/FormAlert'
import { Button } from '@/components/ui/button'
import { RequireAuth } from '@/components/RequireAuth'
import { AirlineAccessContext, Permission, useAirlineAccess, type AirlineAccess } from '@/features/airline-ops/access'
import { useAirlinePermissionNames, useMyAirlines } from '@/features/airline-ops/api'
import { DeparturesPanel } from '@/features/airline-ops/DeparturesPanel'
import { FlightsPanel } from '@/features/airline-ops/FlightsPanel'
import { SchedulesPanel } from '@/features/airline-ops/SchedulesPanel'
import { toApiError } from '@/lib/api'
import { cn } from '@/lib/utils'

const TABS = [
  { path: 'departures', label: 'Departures', read: Permission.DepartureRead },
  { path: 'schedules', label: 'Schedules', read: Permission.ScheduleRead },
  { path: 'flights', label: 'Flights', read: Permission.FlightRead },
] as const

export function AirlineWorkspace() {
  return (
    <RequireAuth>
      <Workspace />
    </RequireAuth>
  )
}

function Workspace() {
  const { airlineId = '' } = useParams()
  const airlines = useMyAirlines()
  const permissions = useAirlinePermissionNames(airlineId)
  const airline = airlines.data?.find((candidate) => candidate.id === airlineId)

  if (airlines.isPending || (permissions.isPending && !permissions.isError)) {
    return <Shell><p role="status" className="text-muted-foreground">Loading the airline…</p></Shell>
  }

  if (!airline || toApiError(permissions.error).status === 403) {
    return (
      <Shell>
        <title>Airline | Contrail</title>
        <h1 className="font-display text-5xl">No access to this airline</h1>
        <p className="mt-4 max-w-[60ch] text-muted-foreground">
          {airline && airline.status !== 'ACTIVE'
            ? `${airline.name} isn’t active, so its flights can’t be managed.`
            : 'You aren’t an active member of this airline. If you should be, ask one of its admins to invite you.'}
        </p>
        <Link to="/airline" className="mt-6 inline-block font-medium text-chart-blue underline underline-offset-4">
          Your airlines
        </Link>
      </Shell>
    )
  }

  if (permissions.isError) {
    return (
      <Shell>
        <div className="grid max-w-xl justify-items-start gap-3">
          <FormAlert tone="error" title="Your access couldn’t be checked" focusOnMount={false}>
            {toApiError(permissions.error).message}
          </FormAlert>
          <Button variant="outline" onClick={() => permissions.refetch()}>
            Try again
          </Button>
        </div>
      </Shell>
    )
  }

  const granted = new Set(permissions.data)
  const access: AirlineAccess = { airline, can: (permission) => granted.has(permission) }

  return (
    <AirlineAccessContext value={access}>
      <Shell>
        <title>{`${airline.name} | Contrail`}</title>
        <header className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <Link to="/airline" className="text-sm font-medium text-muted-foreground hover:text-foreground">
              Your airlines
            </Link>
            <h1 className="mt-1 font-display text-[clamp(2.5rem,6vw,3.75rem)]">{airline.name}</h1>
            <AccessSummary />
          </div>
        </header>
        <WorkspaceTabs />
        <div className="mt-8">
          <Outlet />
        </div>
      </Shell>
    </AirlineAccessContext>
  )
}

function Shell({ children }: { children: React.ReactNode }) {
  return <div className="mx-auto max-w-6xl px-5 pt-8 pb-20 sm:px-8 sm:pt-12">{children}</div>
}

function WorkspaceTabs() {
  const { can } = useAirlineAccess()
  const tabs = TABS.filter((tab) => can(tab.read))
  if (tabs.length === 0) return null
  return (
    <nav aria-label="Flight operations" className="mt-8 flex gap-1 border-b">
      {tabs.map((tab) => (
        <NavLink
          key={tab.path}
          to={tab.path}
          className={({ isActive }) =>
            cn(
              '-mb-px border-b-2 px-4 py-2.5 text-sm font-medium',
              isActive ? 'border-primary text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground',
            )
          }
        >
          {tab.label}
        </NavLink>
      ))}
    </nav>
  )
}

/** One line on what this person's role lets them do here, so a missing button never looks like a bug. */
function AccessSummary() {
  const { can } = useAirlineAccess()
  const manages = [
    can(Permission.FlightManage) && 'flights',
    can(Permission.ScheduleManage) && 'schedules',
    can(Permission.DepartureManage) && 'departures',
  ].filter(Boolean) as string[]
  const readsAnything = TABS.some((tab) => can(tab.read))

  let summary: string
  if (!readsAnything) summary = 'Your role here doesn’t include flight operations.'
  else if (manages.length === 0) summary = 'You can view flight operations here, but not change them.'
  else summary = `You can manage ${listOf(manages)} here.`
  if (can(Permission.AircraftAssign)) summary += ' You can also change the aircraft on a departure.'
  else if (manages.includes('departures')) summary += ' Changing a departure’s aircraft is done by fleet assignment.'

  return <p className="mt-2 max-w-[70ch] text-muted-foreground">{summary}</p>
}

function listOf(items: string[]) {
  return items.length <= 1 ? items.join('') : `${items.slice(0, -1).join(', ')} and ${items.at(-1)}`
}

/** The workspace's landing tab: the first section this role can read. */
export function WorkspaceIndex() {
  const { can } = useAirlineAccess()
  const first = TABS.find((tab) => can(tab.read))
  if (!first) {
    return (
      <p className="max-w-[60ch] text-muted-foreground">
        Ask an airline admin for a flight operations role, such as flight dispatcher, if you need access.
      </p>
    )
  }
  return <Navigate to={first.path} replace />
}

/** Each tab is only reachable with its read permission, even by typing the address. */
function RequireAirlinePermission({ permission, children }: { permission: Permission; children: React.ReactNode }) {
  const { can } = useAirlineAccess()
  if (!can(permission)) {
    return <p className="max-w-[60ch] text-muted-foreground">Your role at this airline can’t view this section.</p>
  }
  return children
}

export function DeparturesSection() {
  return (
    <RequireAirlinePermission permission={Permission.DepartureRead}>
      <DeparturesPanel />
    </RequireAirlinePermission>
  )
}

export function SchedulesSection() {
  return (
    <RequireAirlinePermission permission={Permission.ScheduleRead}>
      <SchedulesPanel />
    </RequireAirlinePermission>
  )
}

export function FlightsSection() {
  return (
    <RequireAirlinePermission permission={Permission.FlightRead}>
      <FlightsPanel />
    </RequireAirlinePermission>
  )
}
