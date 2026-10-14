import { Link, Navigate } from 'react-router'
import { FormAlert } from '@/components/form/FormAlert'
import { Button } from '@/components/ui/button'
import { RequireAuth } from '@/components/RequireAuth'
import { useMyAirlines, type Airline } from '@/features/airline-ops/api'
import { toApiError } from '@/lib/api'

export function AirlineHomePage() {
  return (
    <RequireAuth>
      <AirlineChooser />
    </RequireAuth>
  )
}

const STATUS_NOTE: Record<Airline['status'], string | null> = {
  ACTIVE: null,
  SUSPENDED: 'Suspended by Contrail. Its flights can’t be managed or sold.',
  BANNED: 'Banned by Contrail. Its flights can’t be managed or sold.',
  INACTIVE: 'Closed.',
}

/** Where airline staff start: straight into their airline, or a choice when they work for more than one. */
function AirlineChooser() {
  const airlines = useMyAirlines()

  if (airlines.isPending) {
    return (
      <p role="status" className="mx-auto max-w-6xl px-5 pt-14 text-muted-foreground sm:px-8">
        Loading your airlines…
      </p>
    )
  }

  const list = airlines.data ?? []
  if (list.length === 1 && list[0].status === 'ACTIVE') return <Navigate to={`/airline/${list[0].id}`} replace />

  return (
    <section aria-labelledby="airlines-title" className="mx-auto max-w-6xl px-5 pt-8 pb-20 sm:px-8 sm:pt-14">
      <title>Your airlines | Contrail</title>
      <h1 id="airlines-title" className="font-display text-[clamp(2.5rem,6vw,3.75rem)]">
        Your airlines
      </h1>

      {airlines.isError ? (
        <div className="mt-8 grid max-w-xl justify-items-start gap-3">
          <FormAlert tone="error" title="Your airlines couldn’t be loaded" focusOnMount={false}>
            {toApiError(airlines.error).message}
          </FormAlert>
          <Button variant="outline" onClick={() => airlines.refetch()}>
            Try again
          </Button>
        </div>
      ) : list.length === 0 ? (
        <div className="mt-6 max-w-[60ch] text-muted-foreground">
          <p>You aren’t a member of any airline on Contrail.</p>
          <p className="mt-2">
            If you work for an airline, ask one of its admins to invite you. Contrail staff manage airlines from the
            platform admin tools instead.
          </p>
        </div>
      ) : (
        <ul className="mt-8 grid max-w-2xl gap-3">
          {list.map((airline) => {
            const note = STATUS_NOTE[airline.status]
            return (
              <li key={airline.id}>
                <Link
                  to={`/airline/${airline.id}`}
                  className="flex items-center justify-between gap-4 rounded-lg bg-card p-5 shadow-[0_0_0_1px_var(--rule)] hover:shadow-[0_0_0_2px_var(--chart-blue)]"
                >
                  <span>
                    <span className="block font-display text-2xl">{airline.name}</span>
                    <span className="text-sm text-muted-foreground">
                      {[airline.iataCode, airline.icaoCode].filter(Boolean).join(' / ') || 'No airline codes yet'}
                    </span>
                  </span>
                  {note && <span className="max-w-[24ch] text-right text-sm text-muted-foreground">{note}</span>}
                </Link>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
