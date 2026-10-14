import { useEffect, type ReactNode } from 'react'
import { differenceInCalendarDays, format, parseISO } from 'date-fns'
import { Link, useSearchParams } from 'react-router'
import { Button } from '@/components/ui/button'
import { useAirport } from '@/features/flight-search/airports'
import { useFlightSearch, type FlightResult } from '@/features/flight-search/flights'
import { describeTravellers, fromSearchParams, type SearchSummary } from '@/features/flight-search/searchParams'
import { CABIN_LABELS, TRIP_TYPE_LABELS } from '@/features/flight-search/searchSchema'
import { toApiError } from '@/lib/api'

type Leg = { from: string; to: string; date?: Date; label: string }


export function SearchResultsPage() {
  const [params] = useSearchParams()
  const search = fromSearchParams(params)

  return (
    <section aria-labelledby="results-title" className="mx-auto max-w-6xl px-5 py-12 sm:px-8">
      <h1 id="results-title" className="font-display text-5xl">
        Your search
      </h1>
      <p className="mt-2 text-muted-foreground">
        {TRIP_TYPE_LABELS[search.tripType]}, {describeTravellers(search)}, {CABIN_LABELS[search.cabin]}
      </p>
      <Link to="/#search" className="mt-4 inline-block rounded-sm font-medium text-chart-blue underline underline-offset-4">
        Change search
      </Link>

      <div className="mt-10 grid gap-12">
        {legsOf(search).map((leg, index) => (
          <LegResults key={index} leg={leg} search={search} />
        ))}
      </div>
    </section>
  )
}

function legsOf(search: SearchSummary): Leg[] {
  const legs = search.legs.map((leg, index) => ({
    ...leg,
    label: search.tripType === 'multi-city' ? `Flight ${index + 1}` : 'Outbound',
  }))
  const outbound = search.legs[0]
  if (search.tripType === 'return' && outbound) {
    legs.push({ from: outbound.to, to: outbound.from, date: search.returnDate, label: 'Return' })
  }
  return legs
}

function LegResults({ leg, search }: { leg: Leg; search: SearchSummary }) {
  const from = useAirport(leg.from)
  const to = useAirport(leg.to)
  const flights = useFlightSearch({
    departureAirportId: from.data?.id,
    arrivalAirportId: to.data?.id,
    date: leg.date,
    adults: search.adults,
    children: search.children,
    infants: search.infants,
    cabin: search.cabin,
  })
  const { data, hasNextPage, isFetchingNextPage, fetchNextPage } = flights

  // Seat and fare filters run after the server pages, so a page can come back empty while later ones match.
  useEffect(() => {
    if (data?.length === 0 && hasNextPage && !isFetchingNextPage) void fetchNextPage()
  }, [data, hasNextPage, isFetchingNextPage, fetchNextPage])
  const headingId = `leg-${leg.label.replace(/\s+/g, '-').toLowerCase()}`

  return (
    <section aria-labelledby={headingId}>
      <p className="text-sm font-medium tracking-wide text-muted-foreground uppercase">{leg.label}</p>
      <h2 id={headingId} className="mt-1 flex flex-wrap items-baseline gap-x-4 gap-y-1">
        <span className="font-display text-4xl">
          {leg.from} <span className="text-chart-blue">to</span> {leg.to}
        </span>
        <span className="text-muted-foreground">
          {from.data?.cityName ?? leg.from} to {to.data?.cityName ?? leg.to}
          {leg.date && `, ${format(leg.date, 'EEE d MMM yyyy')}`}
        </span>
      </h2>

      <div className="mt-5">
        <LegBody leg={leg} from={from} to={to} flights={flights} travellers={describeTravellers(search)} />
      </div>
    </section>
  )
}

function LegBody({
  leg,
  from,
  to,
  flights,
  travellers,
}: {
  leg: Leg
  from: ReturnType<typeof useAirport>
  to: ReturnType<typeof useAirport>
  flights: ReturnType<typeof useFlightSearch>
  travellers: string
}) {
  if (!leg.date) return <Notice>Choose a departure date to see flights.</Notice>
  if (from.isError || to.isError) return <Notice>{toApiError(from.error ?? to.error).message}</Notice>
  if (from.isPending || to.isPending) return <Notice>Looking up airports…</Notice>
  if (!from.data || !to.data) return <Notice>We don’t recognise one of these airports. Change your search and pick it from the list.</Notice>

  if (flights.isPending) return <Notice>Searching flights…</Notice>
  if (flights.isError) {
    return (
      <div role="alert" className="flex flex-wrap items-center gap-4">
        <p>{toApiError(flights.error).message}</p>
        <Button variant="outline" onClick={() => flights.refetch()}>
          Try again
        </Button>
      </div>
    )
  }
  if (flights.data.length === 0 && (flights.hasNextPage || flights.isFetchingNextPage)) {
    return <Notice>Searching flights…</Notice>
  }
  if (flights.data.length === 0) {
    return <Notice>No flights match this day and cabin. Try another date or cabin.</Notice>
  }

  return (
    <>
      <p role="status" className="sr-only">
        {flights.data.length} flights found
      </p>
      <ul className="grid gap-3">
        {flights.data.map((flight) => (
          <li key={flight.id}>
            <FlightCard flight={flight} travellers={travellers} />
          </li>
        ))}
      </ul>
      {flights.hasNextPage && (
        <Button variant="outline" className="mt-5" onClick={() => flights.fetchNextPage()} disabled={flights.isFetchingNextPage}>
          {flights.isFetchingNextPage ? 'Loading…' : 'Show more flights'}
        </Button>
      )}
    </>
  )
}

function Notice({ children }: { children: ReactNode }) {
  return (
    <p role="status" className="max-w-[60ch] text-muted-foreground">
      {children}
    </p>
  )
}

const LOW_SEATS = 5

function FlightCard({ flight, travellers }: { flight: FlightResult; travellers: string }) {
  const departure = parseISO(flight.departureDateTime)
  const arrival = parseISO(flight.arrivalDateTime)
  const dayOffset = differenceInCalendarDays(arrival, departure)
  const seatsLeft = flight.seatsLeftInCabin

  return (
    <article className="grid gap-4 rounded-lg bg-card p-5 shadow-[0_0_0_1px_var(--rule)] sm:grid-cols-[1fr_auto] sm:items-center">
      <div className="grid gap-3">
        <p className="text-sm text-muted-foreground">
          <span className="font-medium text-foreground">{flight.airlineName ?? 'Airline'}</span> · {flight.flightNumber}
          {flight.aircraftModal && ` · ${flight.aircraftModal}`}
        </p>
        <div className="flex items-center gap-4">
          <Endpoint time={format(departure, 'HH:mm')} code={flight.departureAirport?.iataCode} />
          <div className="flex min-w-16 flex-1 flex-col items-center text-xs text-muted-foreground">
            <span>{flight.formattedDuration}</span>
            <span aria-hidden className="my-1 h-px w-full bg-border" />
            <span>Direct</span>
          </div>
          <Endpoint
            time={format(arrival, 'HH:mm')}
            code={flight.arrivalAirport?.iataCode}
            dayOffset={dayOffset > 0 ? dayOffset : undefined}
          />
        </div>
      </div>

      <div className="flex items-end justify-between gap-4 border-t pt-4 sm:flex-col sm:items-end sm:border-t-0 sm:border-l sm:pt-0 sm:pl-6">
        {flight.fare && flight.partyPrice ? (
          <div className="sm:text-right">
            <p className="font-display text-3xl">{formatPrice(flight.partyPrice.totalPrice, flight.partyPrice.currency)}</p>
            <p className="text-sm text-muted-foreground">Total for {travellers}</p>
            <p className="text-sm text-muted-foreground">
              {formatPrice(flight.fare.totalPrice, flight.fare.currency)} per adult · {flight.fare.fareLabel ?? flight.fare.name}
            </p>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">Price unavailable</p>
        )}
        {seatsLeft != null && seatsLeft <= LOW_SEATS && (
          <p className="text-sm font-medium text-primary">
            {seatsLeft} {seatsLeft === 1 ? 'seat' : 'seats'} left in this cabin
          </p>
        )}
      </div>
    </article>
  )
}

function Endpoint({ time, code, dayOffset }: { time: string; code?: string; dayOffset?: number }) {
  return (
    <div>
      <p className="font-display text-2xl tabular-nums">
        {time}
        {dayOffset && <sup className="ml-0.5 text-xs text-primary">+{dayOffset}</sup>}
      </p>
      <p className="text-sm text-muted-foreground">{code}</p>
    </div>
  )
}

function formatPrice(amount: number, currency: string) {
  try {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(amount)
  } catch {
    return `${currency} ${amount}`
  }
}
