import { useState } from 'react'
import { useForm, useWatch, type UseFormRegister } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { MoreHorizontal, Plus } from 'lucide-react'
import { Field } from '@/components/form/Field'
import { FormAlert } from '@/components/form/FormAlert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import type { ApiError } from '@/lib/api'
import { applyServerErrors } from '@/lib/forms'
import { Permission, useAirlineAccess } from './access'
import {
  useAllFlights,
  useChangeDepartureStatus,
  useCreateDeparture,
  useDeleteDeparture,
  useDepartures,
  useFleet,
  useReassignAircraft,
  useUpdateDeparture,
  type Aircraft,
  type Departure,
  type DepartureFilters,
  type FlightStatus,
} from './api'
import {
  EDITABLE_STATUSES,
  NEXT_STATUSES,
  STATUS_LABELS,
  formatDateTime,
  formatShortDateTime,
  joinDateTime,
  plural,
  routeLabel,
  splitDateTime,
} from './format'
import { ConfirmDialog, DataTable, ListState, Pager, SectionHeader, SelectField, StatusLabel, td, th } from './ui'

type Action = 'edit' | 'status' | 'aircraft' | 'delete'

/** Aircraft can still be swapped up to boarding; after that the departure is flying. */
const REASSIGNABLE: FlightStatus[] = ['SCHEDULED', 'DELAYED', 'BOARDING']

export function DeparturesPanel() {
  const { airline, can } = useAirlineAccess()
  const [filters, setFilters] = useState<DepartureFilters>({})
  const [page, setPage] = useState(0)
  const departures = useDepartures(airline.id, filters, page)
  const flights = useAllFlights(airline.id, can(Permission.FlightRead))
  const [active, setActive] = useState<{ action: Action; departure: Departure } | null>(null)
  const [creating, setCreating] = useState(false)
  const remove = useDeleteDeparture(airline.id)
  const canCreate = can(Permission.DepartureManage) && can(Permission.AircraftRead) && can(Permission.FlightRead)

  const changeFilters = (next: DepartureFilters) => {
    setFilters(next)
    setPage(0)
  }
  const filtered = Boolean(filters.onDate || filters.flightId)

  return (
    <section aria-labelledby="departures-title" className="grid gap-6">
      <SectionHeader
        title="Departures"
        description="Every dated departure, from schedules or added one at a time. Seats sold come from the live seat inventory."
        action={
          canCreate && (
            <Button onClick={() => setCreating(true)}>
              <Plus aria-hidden /> Add a departure
            </Button>
          )
        }
      />

      <div className="flex flex-wrap items-end gap-4">
        <div className="w-44">
          <Field
            id="departures-date"
            label="Date"
            type="date"
            value={filters.onDate ?? ''}
            onChange={(event) => changeFilters({ ...filters, onDate: event.target.value || undefined })}
          />
        </div>
        {can(Permission.FlightRead) && (
          <div className="w-72">
            <SelectField
              id="departures-flight"
              label="Flight"
              value={filters.flightId ?? ''}
              onChange={(event) => changeFilters({ ...filters, flightId: event.target.value || undefined })}
            >
              <option value="">All flights</option>
              {flights.data?.map((flight) => (
                <option key={flight.id} value={flight.id}>
                  {flight.flightNumber}, {routeLabel(flight.departureAirport, flight.arrivalAirport)}
                </option>
              ))}
            </SelectField>
          </div>
        )}
        {filtered && (
          <Button variant="ghost" onClick={() => changeFilters({})}>
            Clear filters
          </Button>
        )}
      </div>

      <ListState
        isLoading={departures.isPending}
        error={departures.error}
        onRetry={() => departures.refetch()}
        isEmpty={departures.data?.items.length === 0}
        empty={
          filtered
            ? 'No departures match these filters.'
            : 'No departures yet. They appear here once a schedule creates them or one is added.'
        }
      >
        <DataTable caption="Departures">
          <thead>
            <tr>
              <th scope="col" className={th}>Departs</th>
              <th scope="col" className={th}>Flight</th>
              <th scope="col" className={th}>Aircraft</th>
              <th scope="col" className={th}>Seats sold</th>
              <th scope="col" className={th}>Status</th>
              <th scope="col" className={th}><span className="sr-only">Actions</span></th>
            </tr>
          </thead>
          <tbody>
            {departures.data?.items.map((departure) => (
              <tr key={departure.id}>
                <td className={`${td} whitespace-nowrap tabular-nums`}>
                  {formatDateTime(departure.departureDateTime)}
                  <span className="block text-muted-foreground">
                    Arrives {formatShortDateTime(departure.arrivalDateTime)}
                  </span>
                </td>
                <td className={td}>
                  <span className="font-semibold">{departure.flightNumber}</span>
                  <span className="block text-muted-foreground">{routeLabel(departure.departureAirport, departure.arrivalAirport)}</span>
                  {!departure.scheduleId && <span className="block text-xs text-muted-foreground">One-off</span>}
                </td>
                <td className={td}>
                  {departure.aircraftCode ?? '—'}
                  {departure.aircraftModal && <span className="block text-muted-foreground">{departure.aircraftModal}</span>}
                </td>
                <td className={`${td} tabular-nums`}>
                  {departure.seatsSold == null ? (
                    <span className="text-muted-foreground" title="Seat inventory couldn’t be read">Unknown</span>
                  ) : (
                    <>
                      {departure.seatsSold} <span className="text-muted-foreground">of {departure.totalSeats}</span>
                    </>
                  )}
                </td>
                <td className={td}>
                  <StatusLabel status={departure.status} />
                </td>
                <td className={`${td} text-right`}>
                  <DepartureActions departure={departure} onAction={(action) => setActive({ action, departure })} />
                </td>
              </tr>
            ))}
          </tbody>
        </DataTable>
        <Pager result={departures.data} onPage={setPage} />
      </ListState>

      {creating && <NewDepartureDialog onClose={() => setCreating(false)} />}
      {active?.action === 'edit' && <EditDepartureDialog departure={active.departure} onClose={() => setActive(null)} />}
      {active?.action === 'status' && <StatusDialog departure={active.departure} onClose={() => setActive(null)} />}
      {active?.action === 'aircraft' && <AircraftDialog departure={active.departure} onClose={() => setActive(null)} />}
      <ConfirmDialog
        open={active?.action === 'delete'}
        onOpenChange={(open) => !open && setActive(null)}
        title={`Delete ${active?.departure.flightNumber ?? ''} on ${active ? formatDateTime(active.departure.departureDateTime) : ''}?`}
        description={<p>The departure and its seat inventory are removed. Only a departure with no seats sold can be deleted.</p>}
        confirmLabel="Delete departure"
        pendingLabel="Deleting…"
        destructive
        onConfirm={() => remove.mutateAsync(active!.departure.id)}
      />
    </section>
  )
}

/** Only the actions this person may take on this departure, in its current state, are listed. */
function DepartureActions({ departure, onAction }: { departure: Departure; onAction: (action: Action) => void }) {
  const { can } = useAirlineAccess()
  const [open, setOpen] = useState(false)
  const manage = can(Permission.DepartureManage)
  const hasSales = (departure.seatsSold ?? 0) > 0
  const actions: { action: Action; label: string; destructive?: boolean }[] = []
  if (manage && EDITABLE_STATUSES.includes(departure.status)) actions.push({ action: 'edit', label: 'Edit times and details' })
  if (manage && NEXT_STATUSES[departure.status].length > 0) actions.push({ action: 'status', label: 'Change status' })
  if (can(Permission.AircraftAssign) && can(Permission.AircraftRead) && REASSIGNABLE.includes(departure.status)) {
    actions.push({ action: 'aircraft', label: 'Change aircraft' })
  }
  if (manage && !hasSales) actions.push({ action: 'delete', label: 'Delete', destructive: true })

  if (actions.length === 0) return null
  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon-sm" aria-label={`Actions for ${departure.flightNumber} on ${formatDateTime(departure.departureDateTime)}`}>
          <MoreHorizontal aria-hidden />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-56 p-1">
        <ul className="grid">
          {actions.map(({ action, label, destructive }) => (
            <li key={action}>
              <button
                type="button"
                className={`w-full rounded-sm px-3 py-2 text-left text-sm hover:bg-accent focus-visible:bg-accent ${destructive ? 'text-destructive' : ''}`}
                onClick={() => {
                  setOpen(false)
                  onAction(action)
                }}
              >
                {label}
              </button>
            </li>
          ))}
        </ul>
        {manage && hasSales && (
          <p className="border-t px-3 py-2 text-xs text-muted-foreground">
            {plural(departure.seatsSold ?? 0, 'seat')} sold, so it can’t be deleted or cancelled here.
          </p>
        )}
      </PopoverContent>
    </Popover>
  )
}

// ---------------------------------------------------------------- shared time fields

const timesSchema = z
  .object({
    departureDate: z.string().min(1, 'Choose the departure date'),
    departureTime: z.string().min(1, 'Enter the departure time'),
    arrivalDate: z.string().min(1, 'Choose the arrival date'),
    arrivalTime: z.string().min(1, 'Enter the arrival time'),
  })
  .superRefine((values, ctx) => {
    if (!values.departureDate || !values.departureTime || !values.arrivalDate || !values.arrivalTime) return
    const departure = joinDateTime(values.departureDate, values.departureTime)
    const arrival = joinDateTime(values.arrivalDate, values.arrivalTime)
    if (arrival <= departure) ctx.addIssue({ code: 'custom', path: ['arrivalTime'], message: 'Arrival must be after departure' })
    if (new Date(departure) <= new Date()) {
      ctx.addIssue({ code: 'custom', path: ['departureTime'], message: 'The departure must be in the future' })
    }
  })

/** API field names point at the time inputs, since dates and times are edited separately. */
const TIME_ALIASES = { departureDateTime: 'departureTime', arrivalDateTime: 'arrivalTime' } as const

type TimeValues = z.infer<typeof timesSchema>

type Registered = ReturnType<UseFormRegister<TimeValues>>

/** Takes the four inputs already registered by the parent form, so any form with these fields can use it. */
function TimeFields({
  fields,
  errors,
}: {
  fields: Record<keyof TimeValues, Registered>
  errors: Partial<Record<keyof TimeValues, { message?: string }>>
}) {
  return (
    <>
      <div className="grid gap-5 sm:grid-cols-2">
        <Field id="departure-date" label="Departure date" type="date" error={errors.departureDate?.message} {...fields.departureDate} />
        <Field id="departure-time" label="Departure time" type="time" error={errors.departureTime?.message} {...fields.departureTime} />
      </div>
      <div className="grid gap-5 sm:grid-cols-2">
        <Field id="arrival-date" label="Arrival date" type="date" error={errors.arrivalDate?.message} {...fields.arrivalDate} />
        <Field
          id="arrival-time"
          label="Arrival time"
          type="time"
          error={errors.arrivalTime?.message}
          hint="Local time at each airport."
          {...fields.arrivalTime}
        />
      </div>
    </>
  )
}

// ---------------------------------------------------------------- edit

const optionalDays = z
  .string()
  .trim()
  .refine((value) => value === '' || /^\d+$/.test(value), 'Enter a whole number of days')

const editSchema = timesSchema.and(
  z
    .object({
      terminal: z.string().trim().max(20, 'Use 20 characters or fewer'),
      gate: z.string().trim().max(20, 'Use 20 characters or fewer'),
      minAdvanceBookingDays: optionalDays,
      maxAdvanceBookingDays: optionalDays,
    })
    .refine(
      (values) =>
        values.minAdvanceBookingDays === '' ||
        values.maxAdvanceBookingDays === '' ||
        Number(values.minAdvanceBookingDays) <= Number(values.maxAdvanceBookingDays),
      { path: ['maxAdvanceBookingDays'], message: 'Sales must open before they close' },
    ),
)

type EditValues = z.infer<typeof editSchema>

function EditDepartureDialog({ departure, onClose }: { departure: Departure; onClose: () => void }) {
  const { airline } = useAirlineAccess()
  const update = useUpdateDeparture(airline.id)
  const [problem, setProblem] = useState<ApiError | null>(null)
  const departs = splitDateTime(departure.departureDateTime)
  const arrives = splitDateTime(departure.arrivalDateTime)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<EditValues>({
    resolver: zodResolver(editSchema),
    defaultValues: {
      departureDate: departs.date,
      departureTime: departs.time,
      arrivalDate: arrives.date,
      arrivalTime: arrives.time,
      terminal: departure.terminal ?? '',
      gate: departure.gate ?? '',
      minAdvanceBookingDays: departure.minAdvanceBookingDays?.toString() ?? '',
      maxAdvanceBookingDays: departure.maxAdvanceBookingDays?.toString() ?? '',
    },
  })

  const onSubmit = handleSubmit((values) => {
    setProblem(null)
    update.mutate(
      {
        id: departure.id,
        input: {
          departureDateTime: joinDateTime(values.departureDate, values.departureTime),
          arrivalDateTime: joinDateTime(values.arrivalDate, values.arrivalTime),
          terminal: values.terminal,
          gate: values.gate,
          minAdvanceBookingDays: values.minAdvanceBookingDays === '' ? null : Number(values.minAdvanceBookingDays),
          maxAdvanceBookingDays: values.maxAdvanceBookingDays === '' ? null : Number(values.maxAdvanceBookingDays),
        },
      },
      {
        onSuccess: onClose,
        onError: (error) =>
          setProblem(
            applyServerErrors(
              error,
              setError,
              ['departureTime', 'arrivalTime', 'terminal', 'gate', 'minAdvanceBookingDays', 'maxAdvanceBookingDays'],
              TIME_ALIASES,
            ),
          ),
      },
    )
  })

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[92svh] overflow-y-auto sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>Edit {departure.flightNumber} on {formatDateTime(departure.departureDateTime)}</DialogTitle>
          <DialogDescription>Route, aircraft and seats aren’t changed here.</DialogDescription>
        </DialogHeader>
        <form id="departure-edit-form" noValidate onSubmit={onSubmit} className="grid gap-5">
          {(departure.seatsSold ?? 0) > 0 && (
            <FormAlert tone="error" title={`${plural(departure.seatsSold ?? 0, 'seat')} already sold`} focusOnMount={false}>
              Passengers aren’t told about a time change automatically. Let them know before you move this departure.
            </FormAlert>
          )}
          {problem && (
            <FormAlert tone="error" title="The departure couldn’t be saved">
              {problem.message}
            </FormAlert>
          )}
          <TimeFields
            fields={{
              departureDate: register('departureDate'),
              departureTime: register('departureTime'),
              arrivalDate: register('arrivalDate'),
              arrivalTime: register('arrivalTime'),
            }}
            errors={errors}
          />
          <div className="grid gap-5 sm:grid-cols-2">
            <Field id="departure-terminal" label="Terminal (optional)" error={errors.terminal?.message} {...register('terminal')} />
            <Field id="departure-gate" label="Gate (optional)" error={errors.gate?.message} {...register('gate')} />
          </div>
          <div className="grid gap-5 sm:grid-cols-2">
            <Field
              id="departure-min-days"
              label="Sales close (days before departure)"
              inputMode="numeric"
              error={errors.minAdvanceBookingDays?.message}
              hint="Leave empty to sell until departure."
              {...register('minAdvanceBookingDays')}
            />
            <Field
              id="departure-max-days"
              label="Sales open (days before departure)"
              inputMode="numeric"
              error={errors.maxAdvanceBookingDays?.message}
              hint="Leave empty to sell as soon as it’s created."
              {...register('maxAdvanceBookingDays')}
            />
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={update.isPending}>
            Cancel
          </Button>
          <Button type="submit" form="departure-edit-form" disabled={update.isPending} aria-busy={update.isPending}>
            {update.isPending ? 'Saving…' : 'Save changes'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ---------------------------------------------------------------- status

const STATUS_HINTS: Partial<Record<FlightStatus, string>> = {
  DELAYED: 'The departure is running late. Edit its times to show the new ones.',
  SCHEDULED: 'Back on time.',
  BOARDING: 'Passengers are boarding.',
  DEPARTED: 'The aircraft has left the gate.',
  CANCELLED: 'The departure won’t operate and is no longer sold. Only possible with no seats sold.',
}

function StatusDialog({ departure, onClose }: { departure: Departure; onClose: () => void }) {
  const { airline } = useAirlineAccess()
  const change = useChangeDepartureStatus(airline.id)
  const options = NEXT_STATUSES[departure.status]
  const hasSales = (departure.seatsSold ?? 0) > 0
  const [choice, setChoice] = useState<FlightStatus | null>(null)

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Change status of {departure.flightNumber}</DialogTitle>
          <DialogDescription>
            Now {STATUS_LABELS[departure.status].toLowerCase()}. A departure only moves forward through its day.
          </DialogDescription>
        </DialogHeader>
        {change.isError && (
          <FormAlert tone="error" title="The status couldn’t be changed">
            {change.error.message}
          </FormAlert>
        )}
        <fieldset className="grid gap-2">
          <legend className="sr-only">New status</legend>
          {options.map((status) => {
            const blocked = status === 'CANCELLED' && hasSales
            return (
              <label
                key={status}
                className={`flex gap-3 rounded-md border p-3 text-sm ${blocked ? 'opacity-60' : 'cursor-pointer has-[:checked]:border-chart-blue has-[:checked]:bg-accent'}`}
              >
                <input type="radio" name="departure-status" disabled={blocked} checked={choice === status} onChange={() => setChoice(status)} className="mt-0.5" />
                <span>
                  <span className="font-medium">{STATUS_LABELS[status]}</span>
                  <span className="block text-muted-foreground">
                    {blocked
                      ? `${plural(departure.seatsSold ?? 0, 'seat')} sold. Passengers have to be rebooked or refunded before it can be cancelled.`
                      : STATUS_HINTS[status]}
                  </span>
                </span>
              </label>
            )
          })}
        </fieldset>
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={change.isPending}>
            Cancel
          </Button>
          <Button
            variant={choice === 'CANCELLED' ? 'destructive' : 'default'}
            disabled={!choice || change.isPending}
            aria-busy={change.isPending}
            onClick={() => choice && change.mutate({ id: departure.id, status: choice }, { onSuccess: onClose })}
          >
            {change.isPending ? 'Saving…' : choice === 'CANCELLED' ? 'Cancel departure' : 'Change status'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ---------------------------------------------------------------- aircraft

function aircraftLabel(aircraft: Aircraft) {
  return `${aircraft.code}, ${aircraft.model}${aircraft.totalSeats ? `, ${aircraft.totalSeats} seats` : ''}`
}

function AircraftDialog({ departure, onClose }: { departure: Departure; onClose: () => void }) {
  const { airline } = useAirlineAccess()
  const fleet = useFleet(airline.id, true)
  const reassign = useReassignAircraft(airline.id)
  const [aircraftId, setAircraftId] = useState('')
  const choices = (fleet.data ?? []).filter((aircraft) => aircraft.id !== departure.aircraftId)

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Change aircraft for {departure.flightNumber}</DialogTitle>
          <DialogDescription>
            Now {departure.aircraftCode ?? 'unknown'}
            {departure.aircraftModal ? `, ${departure.aircraftModal}` : ''}. The new aircraft must have room in every
            cabin for the seats already sold; sold seats keep their numbers where the new layout has them.
          </DialogDescription>
        </DialogHeader>
        {reassign.isError && (
          <FormAlert tone="error" title="The aircraft couldn’t be changed">
            {reassign.error.message}
          </FormAlert>
        )}
        <SelectField
          id="reassign-aircraft"
          label="New aircraft"
          value={aircraftId}
          disabled={fleet.isPending}
          error={fleet.isError ? 'The fleet couldn’t be loaded' : undefined}
          onChange={(event) => setAircraftId(event.target.value)}
        >
          <option value="">{fleet.isPending ? 'Loading fleet…' : 'Choose an aircraft'}</option>
          {choices.map((aircraft) => (
            <option key={aircraft.id} value={aircraft.id} disabled={aircraft.isOperational === false}>
              {aircraftLabel(aircraft)}
              {aircraft.isOperational === false ? ' (not operational)' : ''}
            </option>
          ))}
        </SelectField>
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={reassign.isPending}>
            Cancel
          </Button>
          <Button
            disabled={!aircraftId || reassign.isPending}
            aria-busy={reassign.isPending}
            onClick={() => reassign.mutate({ id: departure.id, aircraftId }, { onSuccess: onClose })}
          >
            {reassign.isPending ? 'Checking seats…' : 'Change aircraft'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ---------------------------------------------------------------- new one-off departure

const newSchema = timesSchema.and(
  z.object({
    flightId: z.string().min(1, 'Choose the flight'),
    aircraftId: z.string().min(1, 'Choose the aircraft'),
  }),
)

type NewValues = z.infer<typeof newSchema>

function NewDepartureDialog({ onClose }: { onClose: () => void }) {
  const { airline } = useAirlineAccess()
  const flights = useAllFlights(airline.id)
  const fleet = useFleet(airline.id, true)
  const create = useCreateDeparture(airline.id)
  const [problem, setProblem] = useState<ApiError | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors },
  } = useForm<NewValues>({
    resolver: zodResolver(newSchema),
    defaultValues: { flightId: '', aircraftId: '', departureDate: '', departureTime: '', arrivalDate: '', arrivalTime: '' },
  })
  const aircraftId = useWatch({ control, name: 'aircraftId' })
  const seats = fleet.data?.find((aircraft) => aircraft.id === aircraftId)?.totalSeats ?? 1

  const onSubmit = handleSubmit((values) => {
    setProblem(null)
    create.mutate(
      {
        flightId: values.flightId,
        aircraftId: values.aircraftId,
        departureDateTime: joinDateTime(values.departureDate, values.departureTime),
        arrivalDateTime: joinDateTime(values.arrivalDate, values.arrivalTime),
        totalSeats: seats,
      },
      {
        onSuccess: onClose,
        onError: (error) =>
          setProblem(applyServerErrors(error, setError, ['flightId', 'aircraftId', 'departureTime', 'arrivalTime'], TIME_ALIASES)),
      },
    )
  })

  const noFlights = flights.data?.length === 0

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[92svh] overflow-y-auto sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>Add a departure</DialogTitle>
          <DialogDescription>A single extra departure for a flight, outside any schedule. Seats come from the aircraft’s layout.</DialogDescription>
        </DialogHeader>
        {noFlights ? (
          <FormAlert tone="error" title="Create a flight first" focusOnMount={false}>
            A departure belongs to a flight. Add one on the Flights page, then come back.
          </FormAlert>
        ) : (
          <form id="departure-new-form" noValidate onSubmit={onSubmit} className="grid gap-5">
            {problem && (
              <FormAlert tone="error" title="The departure couldn’t be added">
                {problem.message}
              </FormAlert>
            )}
            <div className="grid gap-5 sm:grid-cols-2">
              <SelectField id="new-departure-flight" label="Flight" error={errors.flightId?.message} disabled={flights.isPending} {...register('flightId')}>
                <option value="">{flights.isPending ? 'Loading flights…' : 'Choose a flight'}</option>
                {flights.data?.map((flight) => (
                  <option key={flight.id} value={flight.id}>
                    {flight.flightNumber}, {routeLabel(flight.departureAirport, flight.arrivalAirport)}
                  </option>
                ))}
              </SelectField>
              <SelectField
                id="new-departure-aircraft"
                label="Aircraft"
                error={errors.aircraftId?.message ?? (fleet.isError ? 'The fleet couldn’t be loaded' : undefined)}
                disabled={fleet.isPending}
                {...register('aircraftId')}
              >
                <option value="">{fleet.isPending ? 'Loading fleet…' : 'Choose an aircraft'}</option>
                {fleet.data?.map((aircraft) => (
                  <option key={aircraft.id} value={aircraft.id} disabled={aircraft.isOperational === false}>
                    {aircraftLabel(aircraft)}
                    {aircraft.isOperational === false ? ' (not operational)' : ''}
                  </option>
                ))}
              </SelectField>
            </div>
            <TimeFields
            fields={{
              departureDate: register('departureDate'),
              departureTime: register('departureTime'),
              arrivalDate: register('arrivalDate'),
              arrivalTime: register('arrivalTime'),
            }}
            errors={errors}
          />
          </form>
        )}
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={create.isPending}>
            Cancel
          </Button>
          {!noFlights && (
            <Button type="submit" form="departure-new-form" disabled={create.isPending} aria-busy={create.isPending}>
              {create.isPending ? 'Adding…' : 'Add departure'}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
