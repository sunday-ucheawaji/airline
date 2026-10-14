import { useInfiniteQuery } from '@tanstack/react-query'
import { format } from 'date-fns'
import { api } from '@/lib/api'
import type { Cabin } from './searchSchema'

export interface FlightAirport {
  id: string
  iataCode: string
  name: string
  city?: { name: string } | null
}

export interface FlightFare {
  id: string
  name: string
  currency: string
  totalPrice: number
  fareLabel?: string | null
}

export type PassengerType = 'ADT' | 'CHD' | 'INF'

export interface PartyPrice {
  currency: string
  totalPrice: number
  passengers: { passengerType: PassengerType; count: number; fareId: string; unitPrice: number; subtotal: number }[]
}

export interface FlightResult {
  id: string
  flightNumber: string
  airlineName: string | null
  airlineLogo: string | null
  aircraftModal: string | null
  departureAirport: FlightAirport | null
  arrivalAirport: FlightAirport | null
  departureDateTime: string
  arrivalDateTime: string
  formattedDuration: string | null
  availableSeats: number
  seatsLeftInCabin: number | null
  terminal: string | null
  fare: FlightFare | null
  partyPrice: PartyPrice | null
}

// Spring Data serialises a Page either directly or, in DTO mode, with the totals under `page`.
interface FlightPage {
  content: FlightResult[]
  number?: number
  totalPages?: number
  page?: { number: number; totalPages: number }
}

export interface FlightQuery {
  departureAirportId?: string
  arrivalAirportId?: string
  date?: Date
  adults: number
  children: number
  infants: number
  cabin: Cabin
}

const PAGE_SIZE = 20

export function useFlightSearch(query: FlightQuery) {
  const { departureAirportId, arrivalAirportId, date, adults, children, infants, cabin } = query
  const departureDate = date ? format(date, 'yyyy-MM-dd') : undefined

  return useInfiniteQuery({
    queryKey: ['flights', 'search', { departureAirportId, arrivalAirportId, departureDate, adults, children, infants, cabin }],
    queryFn: ({ pageParam, signal }) =>
      api.get<FlightPage>('/api/flights/search', {
        params: {
          departureAirportId,
          arrivalAirportId,
          departureDate,
          adults,
          children,
          infants,
          cabinClass: cabin,
          page: pageParam,
          size: PAGE_SIZE,
        },
        signal,
        skipAuth: true,
      }),
    initialPageParam: 0,
    getNextPageParam: (last) => {
      const number = last.page?.number ?? last.number ?? 0
      const totalPages = last.page?.totalPages ?? last.totalPages ?? 0
      return number + 1 < totalPages ? number + 1 : undefined
    },
    select: (data) => data.pages.flatMap((page) => page.content),
    enabled: Boolean(departureAirportId && arrivalAirportId && departureDate && adults > 0),
    staleTime: 60_000,
  })
}
