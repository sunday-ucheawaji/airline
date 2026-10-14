import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Plus } from 'lucide-react'
import { Field } from '@/components/form/Field'
import { FormAlert } from '@/components/form/FormAlert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import type { ApiError } from '@/lib/api'
import { applyServerErrors } from '@/lib/forms'
import { Permission, useAirlineAccess } from './access'
import { useDeleteFlight, useFlights, useSaveFlight, type Flight } from './api'
import { AirportPicker, type PickedAirport } from './AirportPicker'
import { ConfirmDialog, DataTable, ListState, Pager, SectionHeader, td, th } from './ui'

export function FlightsPanel() {
  const { airline, can } = useAirlineAccess()
  const [page, setPage] = useState(0)
  const flights = useFlights(airline.id, page)
  const canManage = can(Permission.FlightManage)
  const [editing, setEditing] = useState<Flight | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Flight | null>(null)
  const deleteFlight = useDeleteFlight(airline.id)

  return (
    <section aria-labelledby="flights-title" className="grid gap-6">
      <SectionHeader
        title="Flights"
        description="Each flight is a flight number on a route. Schedules and one-off departures are created for a flight."
        action={
          canManage && (
            <Button onClick={() => setEditing('new')}>
              <Plus aria-hidden /> New flight
            </Button>
          )
        }
      />

      <ListState
        isLoading={flights.isPending}
        error={flights.error}
        onRetry={() => flights.refetch()}
        isEmpty={flights.data?.items.length === 0}
        empty={
          canManage
            ? 'No flights yet. Create one to start scheduling departures.'
            : 'This airline has no flights yet.'
        }
      >
        <DataTable caption="Flights">
          <thead>
            <tr>
              <th scope="col" className={th}>Flight</th>
              <th scope="col" className={th}>From</th>
              <th scope="col" className={th}>To</th>
              {canManage && <th scope="col" className={th}><span className="sr-only">Actions</span></th>}
            </tr>
          </thead>
          <tbody>
            {flights.data?.items.map((flight) => (
              <tr key={flight.id}>
                <td className={`${td} font-semibold`}>{flight.flightNumber}</td>
                <td className={td}><AirportCell airport={flight.departureAirport} /></td>
                <td className={td}><AirportCell airport={flight.arrivalAirport} /></td>
                {canManage && (
                  <td className={`${td} text-right whitespace-nowrap`}>
                    <Button variant="ghost" size="sm" onClick={() => setEditing(flight)}>
                      Edit<span className="sr-only"> {flight.flightNumber}</span>
                    </Button>
                    <Button variant="ghost" size="sm" className="text-destructive" onClick={() => setDeleting(flight)}>
                      Delete<span className="sr-only"> {flight.flightNumber}</span>
                    </Button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </DataTable>
        <Pager result={flights.data} onPage={setPage} />
      </ListState>

      {editing && (
        <FlightDialog flight={editing === 'new' ? null : editing} onClose={() => setEditing(null)} />
      )}
      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(open) => !open && setDeleting(null)}
        title={`Delete flight ${deleting?.flightNumber ?? ''}?`}
        description={
          <p>
            Only a flight with no schedules and no departures can be deleted. Delete those first; departures with seats
            sold can’t be deleted at all.
          </p>
        }
        confirmLabel="Delete flight"
        pendingLabel="Deleting…"
        destructive
        onConfirm={() => deleteFlight.mutateAsync(deleting!.id)}
      />
    </section>
  )
}

function AirportCell({ airport }: { airport: Flight['departureAirport'] }) {
  if (!airport) return <span className="text-muted-foreground">Unknown airport</span>
  return (
    <span>
      <span className="font-semibold">{airport.iataCode}</span>{' '}
      <span className="text-muted-foreground">{airport.city?.name ?? airport.name}</span>
    </span>
  )
}

const airportSchema = z.object({ id: z.string(), iataCode: z.string(), name: z.string(), cityName: z.string().optional() })

const flightSchema = z
  .object({
    flightNumber: z
      .string()
      .trim()
      .toUpperCase()
      .min(1, 'Enter the flight number')
      .regex(/^[A-Z0-9]{2}\d{1,4}[A-Z]?$/, 'Use the airline code and number, like GD204'),
    departure: airportSchema.nullable().refine((value) => value !== null, 'Choose where the flight departs from'),
    arrival: airportSchema.nullable().refine((value) => value !== null, 'Choose where the flight arrives'),
  })
  .refine((values) => !values.departure || !values.arrival || values.departure.id !== values.arrival.id, {
    path: ['arrival'],
    message: 'The arrival airport must be different from the departure airport',
  })

type FlightFormInput = z.input<typeof flightSchema>
type FlightFormValues = z.output<typeof flightSchema>

function toPicked(airport: Flight['departureAirport']): PickedAirport | null {
  return airport ? { id: airport.id, iataCode: airport.iataCode, name: airport.name, cityName: airport.city?.name } : null
}

function FlightDialog({ flight, onClose }: { flight: Flight | null; onClose: () => void }) {
  const { airline } = useAirlineAccess()
  const save = useSaveFlight(airline.id)
  const [problem, setProblem] = useState<ApiError | null>(null)
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<FlightFormInput, unknown, FlightFormValues>({
    resolver: zodResolver(flightSchema),
    defaultValues: {
      flightNumber: flight?.flightNumber ?? '',
      departure: toPicked(flight?.departureAirport ?? null),
      arrival: toPicked(flight?.arrivalAirport ?? null),
    },
  })

  const onSubmit = handleSubmit((values) => {
    setProblem(null)
    save.mutate(
      {
        id: flight?.id,
        input: {
          flightNumber: values.flightNumber,
          departureAirportId: values.departure!.id,
          arrivalAirportId: values.arrival!.id,
        },
      },
      {
        onSuccess: onClose,
        onError: (error) =>
          setProblem(
            applyServerErrors(error, setError, ['flightNumber', 'departure', 'arrival'], {
              departureAirportId: 'departure',
              arrivalAirportId: 'arrival',
            }),
          ),
      },
    )
  })

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{flight ? `Edit flight ${flight.flightNumber}` : 'New flight'}</DialogTitle>
          <DialogDescription>
            {flight
              ? 'The airports can only change while the flight has no departures.'
              : 'Flight numbers are unique across every airline.'}
          </DialogDescription>
        </DialogHeader>
        <form id="flight-form" noValidate onSubmit={onSubmit} className="grid gap-5">
          {problem && (
            <FormAlert tone="error" title={flight ? 'The flight couldn’t be saved' : 'The flight couldn’t be created'}>
              {problem.message}
            </FormAlert>
          )}
          <Field
            id="flight-number"
            label="Flight number"
            autoComplete="off"
            className="uppercase"
            error={errors.flightNumber?.message}
            {...register('flightNumber')}
          />
          <Controller
            control={control}
            name="departure"
            render={({ field, fieldState }) => (
              <AirportPicker id="flight-departure" label="Departs from" value={field.value} onChange={field.onChange} error={fieldState.error?.message} />
            )}
          />
          <Controller
            control={control}
            name="arrival"
            render={({ field, fieldState }) => (
              <AirportPicker id="flight-arrival" label="Arrives at" value={field.value} onChange={field.onChange} error={fieldState.error?.message} />
            )}
          />
        </form>
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={save.isPending}>
            Cancel
          </Button>
          <Button type="submit" form="flight-form" disabled={save.isPending} aria-busy={save.isPending}>
            {save.isPending ? 'Saving…' : flight ? 'Save flight' : 'Create flight'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
