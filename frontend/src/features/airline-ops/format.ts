import { format, parseISO } from 'date-fns'
import type { AirportSummary, DayOfWeek, FlightStatus } from './api'

/** Departure and arrival times are the airports' local times, sent without a zone; they are shown exactly as stored. */
export function formatDateTime(value: string) {
  return format(parseISO(value), 'EEE d MMM yyyy, HH:mm')
}

export function formatShortDateTime(value: string) {
  return format(parseISO(value), 'd MMM, HH:mm')
}

export function formatTime(value: string) {
  return value.slice(0, 5)
}

export function formatDate(value: string) {
  return format(parseISO(value), 'd MMM yyyy')
}

/** `yyyy-MM-dd` and `HH:mm` from a LocalDateTime string, for date and time inputs. */
export function splitDateTime(value: string) {
  return { date: value.slice(0, 10), time: value.slice(11, 16) }
}

export function joinDateTime(date: string, time: string) {
  return `${date}T${time}:00`
}

export function airportCode(airport: AirportSummary | null) {
  return airport?.iataCode ?? '—'
}

export function routeLabel(from: AirportSummary | null, to: AirportSummary | null) {
  return `${airportCode(from)} to ${airportCode(to)}`
}

export const STATUS_LABELS: Record<FlightStatus, string> = {
  SCHEDULED: 'Scheduled',
  DELAYED: 'Delayed',
  BOARDING: 'Boarding',
  DEPARTED: 'Departed',
  IN_AIR: 'In the air',
  LANDED: 'Landed',
  ARRIVED: 'Arrived',
  DIVERTED: 'Diverted',
  CANCELLED: 'Cancelled',
  COMPLETED: 'Completed',
}

/** Mirrors flight-ops' lifecycle (FlightInstanceServiceImpl.NEXT_STATUSES): only these moves are offered. */
export const NEXT_STATUSES: Record<FlightStatus, FlightStatus[]> = {
  SCHEDULED: ['DELAYED', 'BOARDING', 'CANCELLED'],
  DELAYED: ['SCHEDULED', 'BOARDING', 'CANCELLED'],
  BOARDING: ['DEPARTED', 'DELAYED', 'CANCELLED'],
  DEPARTED: ['IN_AIR'],
  IN_AIR: ['LANDED', 'DIVERTED'],
  DIVERTED: ['LANDED'],
  LANDED: ['ARRIVED'],
  ARRIVED: ['COMPLETED'],
  CANCELLED: [],
  COMPLETED: [],
}

/** Only a departure that hasn't started can have its times or details edited. */
export const EDITABLE_STATUSES: FlightStatus[] = ['SCHEDULED', 'DELAYED']

export const DAYS: { value: DayOfWeek; short: string; long: string }[] = [
  { value: 'MONDAY', short: 'Mon', long: 'Monday' },
  { value: 'TUESDAY', short: 'Tue', long: 'Tuesday' },
  { value: 'WEDNESDAY', short: 'Wed', long: 'Wednesday' },
  { value: 'THURSDAY', short: 'Thu', long: 'Thursday' },
  { value: 'FRIDAY', short: 'Fri', long: 'Friday' },
  { value: 'SATURDAY', short: 'Sat', long: 'Saturday' },
  { value: 'SUNDAY', short: 'Sun', long: 'Sunday' },
]

export function describeDays(days: DayOfWeek[] | null) {
  if (!days || days.length === 7) return 'Every day'
  return DAYS.filter((day) => days.includes(day.value))
    .map((day) => day.short)
    .join(', ')
}

export function plural(count: number, one: string, many = `${one}s`) {
  return `${count} ${count === 1 ? one : many}`
}
