import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import { FlightSearchForm } from '@/features/flight-search/FlightSearchForm'
import { ManageBookingForm } from '@/features/manage-booking/ManageBookingForm'

const bookingFeatures = [
  {
    title: 'Choose your seat',
    body: 'See the seat map for your flight and pick where each traveller sits.',
  },
  {
    title: 'Add bags and meals',
    body: 'Add checked bags, meals and travel protection when you book, or later from Manage booking.',
  },
  {
    title: 'Know the rules before you pay',
    body: 'Every fare shows whether it’s refundable, what a change costs and the deadline for each.',
  },
  {
    title: 'Change or cancel',
    body: 'Move to another flight with the same airline, or cancel and get back what your fare allows.',
  },
  {
    title: 'Bring the family',
    body: 'Children get their own fare. Infants under two can travel on an adult’s lap.',
  },
  {
    title: 'Pay in the fare’s currency',
    body: 'Pay securely by card in the currency the fare is sold in, with no conversion added.',
  },
]

const bookingSteps = [
  { title: 'Search', body: 'Tell us where and when. No account needed.' },
  { title: 'Choose a fare', body: 'Compare airlines, times and fare rules side by side.' },
  { title: 'Pick seats and extras', body: 'Choose seats, bags and meals for each traveller.' },
  { title: 'Pay', body: 'Pay by card in the fare’s currency.' },
  { title: 'Get your e-ticket', body: 'It arrives by email with your booking reference.' },
]

const airlineSteps = ['Apply', 'Review', 'Approval', 'Go live']

export function LandingPage() {
  return (
    <>
      <section id="search" aria-labelledby="hero-title" className="mx-auto max-w-6xl scroll-mt-6 px-5 pt-8 sm:px-8 sm:pt-14">
        <h1 id="hero-title" className="max-w-[17ch] font-display text-[clamp(2.75rem,6.5vw,4.75rem)]">
          Every airline’s flights in one search.
        </h1>
        <p className="mt-5 max-w-[52ch] text-lg text-muted-foreground">
          Compare fares across airlines, pick your seat and book without creating an account. Your e-ticket arrives
          by email.
        </p>
        <div className="mt-10">
          <FlightSearchForm />
        </div>
      </section>

      <section
        id="manage-booking"
        aria-labelledby="manage-title"
        className="mx-auto mt-14 grid max-w-6xl scroll-mt-6 gap-x-12 gap-y-4 px-5 sm:px-8 lg:grid-cols-[16rem_minmax(0,1fr)]"
      >
        <div>
          <h2 id="manage-title" className="text-xl font-semibold">
            Already booked?
          </h2>
          <p className="mt-1 text-muted-foreground">Find your trip to add extras, change or cancel.</p>
        </div>
        <ManageBookingForm />
      </section>

      <section
        aria-labelledby="features-title"
        className="mx-auto mt-24 grid max-w-6xl gap-x-12 gap-y-10 px-5 sm:px-8 lg:grid-cols-[16rem_minmax(0,1fr)]"
      >
        <h2 id="features-title" className="font-display text-[2.5rem] sm:text-5xl">
          The whole trip in one booking
        </h2>
        <dl className="grid gap-x-12 gap-y-8 sm:grid-cols-2">
          {bookingFeatures.map((feature) => (
            <div key={feature.title} className="border-l-2 border-chart-blue/30 pl-5">
              <dt className="text-lg font-semibold">{feature.title}</dt>
              <dd className="mt-1 max-w-[42ch] text-muted-foreground">{feature.body}</dd>
            </div>
          ))}
        </dl>
      </section>

      <section aria-labelledby="steps-title" className="mx-auto mt-24 max-w-6xl px-5 sm:px-8">
        <h2 id="steps-title" className="font-display text-[2.5rem] sm:text-5xl">
          How booking works
        </h2>
        <ol className="relative mt-10 grid gap-8 md:grid-cols-5 md:gap-6">
          <span
            aria-hidden
            className="absolute top-5 bottom-5 left-5 border-l-2 border-dashed border-chart-blue/40 md:top-5 md:right-[calc((100%-6rem)/5-1.25rem)] md:bottom-auto md:left-5 md:border-t-2 md:border-l-0"
          />
          {bookingSteps.map((step, index) => (
            <li key={step.title} className="relative grid grid-cols-[2.5rem_minmax(0,1fr)] gap-4 md:grid-cols-1">
              <span className="relative flex size-10 items-center justify-center rounded-full border-2 border-chart-blue bg-background font-display text-xl text-chart-blue">
                {index + 1}
              </span>
              <div>
                <h3 className="text-lg font-semibold">{step.title}</h3>
                <p className="mt-1 text-muted-foreground">{step.body}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      <section id="airlines" aria-labelledby="airlines-title" className="mt-28 scroll-mt-6 bg-chart-blue text-white">
        <div className="mx-auto grid max-w-6xl gap-10 px-5 py-16 sm:px-8 sm:py-20 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)] lg:items-end">
          <div>
            <h2 id="airlines-title" className="max-w-[16ch] font-display text-[2.5rem] sm:text-5xl">
              Sell your flights through Contrail
            </h2>
            <p className="mt-5 max-w-[52ch] text-lg text-white/85">
              Airlines apply once. Our team reviews your compliance, commercial and technical details, then sets up
              your account so you can publish schedules, fares and seat maps, and invite your own staff.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-x-6 gap-y-3">
              <Button asChild size="lg" className="h-12 bg-white px-6 text-base font-semibold text-foreground hover:bg-white/90">
                <Link to="/airlines/apply">Apply to join</Link>
              </Button>
              <Link to="/sign-in" className="rounded-sm font-medium text-white underline underline-offset-4 hover:text-white/85">
                Sign in to your airline account
              </Link>
            </div>
          </div>
          <div>
            <h3 className="text-sm font-medium text-white/80">From application to your first sale</h3>
            <ol className="mt-4 grid grid-cols-2 gap-px overflow-hidden rounded-lg bg-white/25 sm:grid-cols-4">
              {airlineSteps.map((step, index) => (
                <li key={step} className="bg-chart-blue p-4">
                  <span className="font-display text-3xl text-white/60">{index + 1}</span>
                  <p className="mt-2 font-semibold">{step}</p>
                </li>
              ))}
            </ol>
          </div>
        </div>
      </section>
    </>
  )
}
