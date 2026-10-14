import { format, parseISO } from 'date-fns'
import { CABINS, TRIP_TYPES, type Cabin, type FlightSearch, type TripType } from './searchSchema'

const DATE_FORMAT = 'yyyy-MM-dd'

export function toSearchParams(search: FlightSearch) {
  const params = new URLSearchParams({ trip: search.tripType })
  for (const leg of search.legs) {
    params.append('leg', `${leg.from}-${leg.to}-${leg.date ? format(leg.date, DATE_FORMAT) : ''}`)
  }
  if (search.tripType === 'return' && search.returnDate) {
    params.set('return', format(search.returnDate, DATE_FORMAT))
  }
  params.set('adults', String(search.adults))
  params.set('children', String(search.children))
  params.set('infants', String(search.infants))
  params.set('cabin', search.cabin)
  return params
}

export type SearchSummary = {
  tripType: TripType
  legs: { from: string; to: string; date?: Date }[]
  returnDate?: Date
  adults: number
  children: number
  infants: number
  cabin: Cabin
}

function parseDate(value: string | null | undefined) {
  if (!value) return undefined
  const date = parseISO(value)
  return Number.isNaN(date.getTime()) ? undefined : date
}

function parseCount(value: string | null, fallback: number) {
  const count = Number(value)
  return value !== null && Number.isInteger(count) && count >= 0 ? count : fallback
}

export function fromSearchParams(params: URLSearchParams): SearchSummary {
  const trip = params.get('trip')
  const cabin = params.get('cabin')
  return {
    tripType: TRIP_TYPES.includes(trip as TripType) ? (trip as TripType) : 'one-way',
    legs: params.getAll('leg').map((leg) => {
      const [from = '', to = '', ...date] = leg.split('-')
      return { from, to, date: parseDate(date.join('-')) }
    }),
    returnDate: parseDate(params.get('return')),
    adults: parseCount(params.get('adults'), 1),
    children: parseCount(params.get('children'), 0),
    infants: parseCount(params.get('infants'), 0),
    cabin: CABINS.includes(cabin as Cabin) ? (cabin as Cabin) : 'ECONOMY',
  }
}

export function describeTravellers({ adults, children, infants }: Pick<SearchSummary, 'adults' | 'children' | 'infants'>) {
  const parts = [`${adults} ${adults === 1 ? 'adult' : 'adults'}`]
  if (children) parts.push(`${children} ${children === 1 ? 'child' : 'children'}`)
  if (infants) parts.push(`${infants} ${infants === 1 ? 'infant' : 'infants'}`)
  return parts.join(', ')
}
