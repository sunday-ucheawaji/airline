import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { format } from 'date-fns'
import { Plus, X } from 'lucide-react'
import { Field } from '@/components/form/Field'
import { FormAlert } from '@/components/form/FormAlert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import type { ApiError } from '@/lib/api'
import { applyServerErrors } from '@/lib/forms'
import { cn } from '@/lib/utils'
import { Permission, useAirlineAccess } from './access'
import {
  useAllFlights,
  useCreateSchedule,
  useDeleteSchedule,
  useFleet,
  useSchedules,
  useUpdateSchedule,
  type DayOfWeek,
  type Schedule,
  type ScheduleChange,
} from './api'
import { DAYS, describeDays, formatDate, formatTime, plural, routeLabel } from './format'
import { ConfirmDialog, DataTable, ListState, SectionHeader, SelectField, td, th } from './ui'

export function SchedulesPanel() {
  const { airline, can } = useAirlineAccess()
  const schedules = useSchedules(airline.id)
  const canManage = can(Permission.ScheduleManage)
  const [editing, setEditing] = useState<Schedule | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Schedule | null>(null)
  const [outcome, setOutcome] = useState<{ title: string; change?: ScheduleChange } | null>(null)
  const deleteSchedule = useDeleteSchedule(airline.id)

  const sorted = [...(schedules.data ?? [])].sort(
    (a, b) => a.flightNumber.localeCompare(b.flightNumber) || a.startDate.localeCompare(b.startDate),
  )

  return (
    <section aria-labelledby="schedules-title" className="grid gap-6">
      <SectionHeader
        title="Schedules"
        description="A schedule creates a departure on each of its days between two dates. Changing it only affects future departures with no seats sold."
        action={
          canManage && (
            <Button onClick={() => setEditing('new')}>
              <Plus aria-hidden /> New schedule
            </Button>
          )
        }
      />

      {outcome && <ScheduleOutcome title={outcome.title} change={outcome.change} onDismiss={() => setOutcome(null)} />}

      <ListState
        isLoading={schedules.isPending}
        error={schedules.error}
        onRetry={() => schedules.refetch()}
        isEmpty={sorted.length === 0}
        empty={canManage ? 'No schedules yet. Create one to generate departures for a flight.' : 'This airline has no schedules yet.'}
      >
        <DataTable caption="Schedules">
          <thead>
            <tr>
              <th scope="col" className={th}>Flight</th>
              <th scope="col" className={th}>Times</th>
              <th scope="col" className={th}>Days</th>
              <th scope="col" className={th}>Dates</th>
              <th scope="col" className={th}>State</th>
              {canManage && <th scope="col" className={th}><span className="sr-only">Actions</span></th>}
            </tr>
          </thead>
          <tbody>
            {sorted.map((schedule) => (
              <tr key={schedule.id}>
                <td className={td}>
                  <span className="font-semibold">{schedule.flightNumber}</span>
                  <span className="block text-muted-foreground">{routeLabel(schedule.departureAirport, schedule.arrivalAirport)}</span>
                </td>
                <td className={`${td} tabular-nums`}>
                  {formatTime(schedule.departureTime)} to {formatTime(schedule.arrivalTime)}
                  {schedule.arrivalTime <= schedule.departureTime && (
                    <span className="block text-muted-foreground">Arrives next day</span>
                  )}
                </td>
                <td className={td}>{describeDays(schedule.operatingDays)}</td>
                <td className={td}>
                  {formatDate(schedule.startDate)} to {formatDate(schedule.endDate)}
                </td>
                <td className={td}>{schedule.isActive ? 'Running' : <span className="text-muted-foreground">Paused</span>}</td>
                {canManage && (
                  <td className={`${td} text-right whitespace-nowrap`}>
                    <Button variant="ghost" size="sm" onClick={() => setEditing(schedule)}>
                      Edit<span className="sr-only"> schedule for {schedule.flightNumber}</span>
                    </Button>
                    <Button variant="ghost" size="sm" className="text-destructive" onClick={() => setDeleting(schedule)}>
                      Delete<span className="sr-only"> schedule for {schedule.flightNumber}</span>
                    </Button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </DataTable>
      </ListState>

      {editing && (
        <ScheduleDialog
          schedule={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onChanged={(title, change) => setOutcome({ title, change })}
        />
      )}
      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(open) => !open && setDeleting(null)}
        title={`Delete this schedule for ${deleting?.flightNumber ?? ''}?`}
        description={
          <>
            <p>Its future departures with no seats sold will be removed.</p>
            <p>Departures that already have seats sold, and past departures, stay as one-off departures.</p>
          </>
        }
        confirmLabel="Delete schedule"
        pendingLabel="Deleting…"
        destructive
        onConfirm={async () => {
          const change = await deleteSchedule.mutateAsync(deleting!.id)
          setOutcome({ title: `Schedule for ${deleting!.flightNumber} deleted`, change })
        }}
      />
    </section>
  )
}

function ScheduleOutcome({ title, change, onDismiss }: { title: string; change?: ScheduleChange; onDismiss: () => void }) {
  return (
    <div className="relative">
      <FormAlert tone="success" title={title}>
        {!change ? (
          <p>Departures were created for each scheduled day. They’re on the Departures page.</p>
        ) : (
        <>
        <p>
          {change.departuresCreated > 0 && `${plural(change.departuresCreated, 'departure')} created. `}
          {change.departuresRemoved > 0 && `${plural(change.departuresRemoved, 'unsold departure')} removed. `}
          {change.departuresCreated === 0 && change.departuresRemoved === 0 && 'No departures needed to change. '}
        </p>
        {change.departuresKeptWithSales > 0 && (
          <p className="mt-1 font-medium text-foreground">
            {plural(change.departuresKeptWithSales, 'departure')} with seats sold kept their original date and time.
            Review them on the Departures page.
          </p>
        )}
        </>
        )}
      </FormAlert>
      <Button variant="ghost" size="icon-sm" className="absolute top-2 right-2" onClick={onDismiss} aria-label="Dismiss">
        <X aria-hidden />
      </Button>
    </div>
  )
}

const today = () => format(new Date(), 'yyyy-MM-dd')

const scheduleSchema = z
  .object({
    flightId: z.string().min(1, 'Choose the flight'),
    aircraftId: z.string().min(1, 'Choose the aircraft'),
    departureTime: z.string().min(1, 'Enter the departure time'),
    arrivalTime: z.string().min(1, 'Enter the arrival time'),
    startDate: z.string().min(1, 'Choose the first date'),
    endDate: z.string().min(1, 'Choose the last date'),
    everyDay: z.boolean(),
    operatingDays: z.array(z.string()),
    isActive: z.boolean(),
  })
  .superRefine((values, ctx) => {
    if (values.departureTime && values.departureTime === values.arrivalTime) {
      ctx.addIssue({ code: 'custom', path: ['arrivalTime'], message: 'Arrival can’t be at the same time as departure' })
    }
    if (values.startDate && values.endDate && values.endDate < values.startDate) {
      ctx.addIssue({ code: 'custom', path: ['endDate'], message: 'The last date must be on or after the first' })
    }
    if (values.endDate && values.endDate < today()) {
      ctx.addIssue({ code: 'custom', path: ['endDate'], message: 'The last date has already passed' })
    }
    if (!values.everyDay && values.operatingDays.length === 0) {
      ctx.addIssue({ code: 'custom', path: ['operatingDays'], message: 'Choose at least one day' })
    }
  })

type ScheduleFormValues = z.infer<typeof scheduleSchema>

type ScheduleDialogProps = {
  schedule: Schedule | null
  onClose: () => void
  onChanged: (title: string, change?: ScheduleChange) => void
}

function ScheduleDialog({ schedule, onClose, onChanged }: ScheduleDialogProps) {
  const { airline, can } = useAirlineAccess()
  const flights = useAllFlights(airline.id)
  const canReadFleet = can(Permission.AircraftRead)
  const fleet = useFleet(airline.id, canReadFleet)
  const create = useCreateSchedule(airline.id)
  const update = useUpdateSchedule(airline.id)
  const saving = create.isPending || update.isPending
  const [problem, setProblem] = useState<ApiError | null>(null)
  const isEveryDay = !schedule || !schedule.operatingDays || schedule.operatingDays.length === 7

  const {
    register,
    control,
    handleSubmit,
    setError,
    setValue,
    formState: { errors },
  } = useForm<ScheduleFormValues>({
    resolver: zodResolver(scheduleSchema),
    defaultValues: {
      flightId: schedule?.flightId ?? '',
      aircraftId: '',
      departureTime: schedule ? formatTime(schedule.departureTime) : '',
      arrivalTime: schedule ? formatTime(schedule.arrivalTime) : '',
      startDate: schedule?.startDate ?? today(),
      endDate: schedule?.endDate ?? '',
      everyDay: isEveryDay,
      operatingDays: isEveryDay ? [] : (schedule?.operatingDays ?? []),
      isActive: schedule?.isActive ?? true,
    },
  })
  const everyDay = useWatch({ control, name: 'everyDay' })
  const departureTime = useWatch({ control, name: 'departureTime' })
  const arrivalTime = useWatch({ control, name: 'arrivalTime' })
  const overnight = Boolean(departureTime && arrivalTime && arrivalTime < departureTime)

  const onSubmit = handleSubmit((values) => {
    setProblem(null)
    const input = {
      flightId: values.flightId,
      aircraftId: values.aircraftId,
      departureTime: values.departureTime,
      arrivalTime: values.arrivalTime,
      startDate: values.startDate,
      endDate: values.endDate,
      recurrenceType: values.everyDay ? ('DAILY' as const) : ('WEEKLY' as const),
      operatingDays: values.everyDay ? [] : (values.operatingDays as DayOfWeek[]),
      isActive: values.isActive,
    }
    const onError = (error: unknown) =>
      setProblem(
        applyServerErrors(error, setError, ['flightId', 'aircraftId', 'departureTime', 'arrivalTime', 'startDate', 'endDate', 'operatingDays']),
      )
    if (schedule) {
      update.mutate(
        { id: schedule.id, input },
        {
          onSuccess: (change) => {
            onChanged(`Schedule for ${schedule.flightNumber} updated`, change)
            onClose()
          },
          onError,
        },
      )
    } else {
      create.mutate(input, {
        onSuccess: (created) => {
          onChanged(`Schedule for ${created.flightNumber} created`)
          onClose()
        },
        onError,
      })
    }
  })

  const noFlights = flights.data?.length === 0
  const fleetOptions = fleet.data ?? []

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[92svh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{schedule ? `Edit schedule for ${schedule.flightNumber}` : 'New schedule'}</DialogTitle>
          <DialogDescription>
            {schedule
              ? 'Future departures with no seats sold are rebuilt from these settings. Departures with seats sold keep their date and time.'
              : 'A departure is created for each chosen day from the first date (or today) to the last.'}
          </DialogDescription>
        </DialogHeader>

        {!canReadFleet ? (
          <FormAlert tone="error" title="You can’t choose an aircraft" focusOnMount={false}>
            Your role at {airline.name} can’t view the fleet, so it can’t create or change schedules. Ask an airline
            admin for a role with fleet access.
          </FormAlert>
        ) : noFlights ? (
          <FormAlert tone="error" title="Create a flight first" focusOnMount={false}>
            A schedule belongs to a flight. Add one on the Flights page, then come back.
          </FormAlert>
        ) : (
          <form id="schedule-form" noValidate onSubmit={onSubmit} className="grid gap-5">
            {problem && (
              <FormAlert tone="error" title="The schedule couldn’t be saved">
                {problem.message}
              </FormAlert>
            )}
            <div className="grid gap-5 sm:grid-cols-2">
              <SelectField
                id="schedule-flight"
                label="Flight"
                error={errors.flightId?.message}
                disabled={Boolean(schedule) || flights.isPending}
                hint={schedule ? 'A schedule stays with its flight.' : undefined}
                {...register('flightId')}
              >
                <option value="">{flights.isPending ? 'Loading flights…' : 'Choose a flight'}</option>
                {flights.data?.map((flight) => (
                  <option key={flight.id} value={flight.id}>
                    {flight.flightNumber}, {routeLabel(flight.departureAirport, flight.arrivalAirport)}
                  </option>
                ))}
              </SelectField>
              <SelectField
                id="schedule-aircraft"
                label="Aircraft"
                error={errors.aircraftId?.message ?? (fleet.isError ? 'The fleet couldn’t be loaded' : undefined)}
                disabled={fleet.isPending}
                hint={schedule ? 'Used for the departures this change creates.' : undefined}
                {...register('aircraftId')}
              >
                <option value="">{fleet.isPending ? 'Loading fleet…' : 'Choose an aircraft'}</option>
                {fleetOptions.map((aircraft) => (
                  <option key={aircraft.id} value={aircraft.id} disabled={aircraft.isOperational === false}>
                    {aircraft.code}, {aircraft.model}
                    {aircraft.totalSeats ? `, ${aircraft.totalSeats} seats` : ''}
                    {aircraft.isOperational === false ? ' (not operational)' : ''}
                  </option>
                ))}
              </SelectField>
            </div>

            <div className="grid gap-5 sm:grid-cols-2">
              <Field id="schedule-departure-time" label="Departs at" type="time" error={errors.departureTime?.message} {...register('departureTime')} />
              <Field
                id="schedule-arrival-time"
                label="Arrives at"
                type="time"
                error={errors.arrivalTime?.message}
                hint={overnight ? 'Earlier than departure, so it arrives the next day.' : 'Local time at each airport.'}
                {...register('arrivalTime')}
              />
            </div>

            <div className="grid gap-5 sm:grid-cols-2">
              <Field id="schedule-start" label="First date" type="date" error={errors.startDate?.message} {...register('startDate')} />
              <Field id="schedule-end" label="Last date" type="date" error={errors.endDate?.message} {...register('endDate')} />
            </div>

            <fieldset className="grid gap-3">
              <legend className="text-sm font-medium">Days</legend>
              <div className="flex flex-wrap gap-x-6 gap-y-2">
                <label className="flex items-center gap-2 text-sm">
                  <input type="radio" name="schedule-days" checked={everyDay} onChange={() => setValue('everyDay', true)} />
                  Every day
                </label>
                <label className="flex items-center gap-2 text-sm">
                  <input type="radio" name="schedule-days" checked={!everyDay} onChange={() => setValue('everyDay', false)} />
                  Selected days
                </label>
              </div>
              {!everyDay && (
                <Controller
                  control={control}
                  name="operatingDays"
                  render={({ field, fieldState }) => (
                    <div>
                      <div role="group" aria-label="Operating days" className="flex flex-wrap gap-2">
                        {DAYS.map((day) => {
                          const checked = field.value.includes(day.value)
                          return (
                            <label
                              key={day.value}
                              className={cn(
                                'cursor-pointer rounded-md border px-3 py-1.5 text-sm has-[:focus-visible]:ring-[3px] has-[:focus-visible]:ring-ring/50',
                                checked ? 'border-chart-blue bg-accent font-semibold text-chart-blue' : 'border-input bg-card',
                              )}
                            >
                              <input
                                type="checkbox"
                                className="sr-only"
                                checked={checked}
                                onChange={() =>
                                  field.onChange(checked ? field.value.filter((value) => value !== day.value) : [...field.value, day.value])
                                }
                              />
                              <span aria-hidden>{day.short}</span>
                              <span className="sr-only">{day.long}</span>
                            </label>
                          )
                        })}
                      </div>
                      {fieldState.error && <p className="mt-2 text-sm text-destructive">{fieldState.error.message}</p>}
                    </div>
                  )}
                />
              )}
            </fieldset>

            {schedule && (
              <Controller
                control={control}
                name="isActive"
                render={({ field }) => (
                  <label className="flex items-start gap-3 text-sm">
                    <input type="checkbox" className="mt-0.5 size-4" checked={!field.value} onChange={(event) => field.onChange(!event.target.checked)} />
                    <span>
                      <span className="font-medium">Pause this schedule</span>
                      <span className="block text-muted-foreground">
                        Removes its future departures with no seats sold and creates none until it’s resumed.
                      </span>
                    </span>
                  </label>
                )}
              />
            )}
          </form>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={saving}>
            Cancel
          </Button>
          {canReadFleet && !noFlights && (
            <Button type="submit" form="schedule-form" disabled={saving} aria-busy={saving}>
              {saving ? 'Saving…' : schedule ? 'Save and update departures' : 'Create schedule'}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
