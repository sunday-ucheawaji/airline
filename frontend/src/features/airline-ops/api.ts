import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'

// ---------------------------------------------------------------- types (mirroring common-lib DTOs)

export type AirlineStatus = 'ACTIVE' | 'SUSPENDED' | 'INACTIVE' | 'BANNED'

export interface Airline {
  id: string
  name: string
  legalName?: string | null
  iataCode?: string | null
  icaoCode?: string | null
  logoUrl?: string | null
  status: AirlineStatus
}

export interface AirportSummary {
  id: string
  iataCode: string
  name: string
  city?: { name: string } | null
}

export type FlightStatus =
  | 'SCHEDULED'
  | 'DELAYED'
  | 'BOARDING'
  | 'DEPARTED'
  | 'IN_AIR'
  | 'LANDED'
  | 'ARRIVED'
  | 'DIVERTED'
  | 'CANCELLED'
  | 'COMPLETED'

export interface Flight {
  id: string
  flightNumber: string
  departureAirport: AirportSummary | null
  arrivalAirport: AirportSummary | null
  createdAt?: string
}

export type RecurrenceType = 'DAILY' | 'WEEKLY' | 'CUSTOM' | 'NONE'
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export interface Schedule {
  id: string
  flightId: string
  flightNumber: string
  departureAirport: AirportSummary | null
  arrivalAirport: AirportSummary | null
  departureTime: string
  arrivalTime: string
  startDate: string
  endDate: string
  recurrenceType: RecurrenceType
  operatingDays: DayOfWeek[] | null
  isActive: boolean
}

export interface ScheduleChange {
  schedule: Schedule | null
  departuresRemoved: number
  departuresCreated: number
  departuresKeptWithSales: number
}

export interface Departure {
  id: string
  flightId: string
  flightNumber: string
  scheduleId: string | null
  aircraftId: string | null
  aircraftModal: string | null
  aircraftCode: string | null
  departureAirport: AirportSummary | null
  arrivalAirport: AirportSummary | null
  departureDateTime: string
  arrivalDateTime: string
  formattedDuration: string | null
  totalSeats: number
  availableSeats: number
  seatsSold: number | null
  status: FlightStatus
  terminal: string | null
  gate: string | null
  minAdvanceBookingDays: number | null
  maxAdvanceBookingDays: number | null
}

export interface Aircraft {
  id: string
  code: string
  model: string
  manufacturer?: string | null
  totalSeats: number | null
  status?: string | null
  isOperational?: boolean | null
}

interface Page<T> {
  content: T[]
  number?: number
  totalPages?: number
  totalElements?: number
  page?: { number: number; totalPages: number; totalElements: number }
}

export interface PageResult<T> {
  items: T[]
  page: number
  totalPages: number
  totalItems: number
}

function toPageResult<T>(page: Page<T>): PageResult<T> {
  return {
    items: page.content,
    page: page.page?.number ?? page.number ?? 0,
    totalPages: page.page?.totalPages ?? page.totalPages ?? 1,
    totalItems: page.page?.totalElements ?? page.totalElements ?? page.content.length,
  }
}

// ---------------------------------------------------------------- keys

export const airlineKeys = {
  mine: ['airlines', 'mine'] as const,
  permissions: (airlineId: string) => ['airlines', airlineId, 'permissions'] as const,
  flights: (airlineId: string) => ['airlines', airlineId, 'flights'] as const,
  schedules: (airlineId: string) => ['airlines', airlineId, 'schedules'] as const,
  departures: (airlineId: string) => ['airlines', airlineId, 'departures'] as const,
  fleet: (airlineId: string) => ['airlines', airlineId, 'fleet'] as const,
}

// ---------------------------------------------------------------- airlines and access

export function useMyAirlines(enabled = true) {
  return useQuery({
    queryKey: airlineKeys.mine,
    queryFn: ({ signal }) => api.get<Airline[]>('/api/airlines/mine', { signal }),
    staleTime: 5 * 60_000,
    enabled,
  })
}

/** The permissions the signed-in user's role grants at this airline. A non-member gets 403. */
export function useAirlinePermissionNames(airlineId: string) {
  return useQuery({
    queryKey: airlineKeys.permissions(airlineId),
    queryFn: ({ signal }) => api.get<string[]>(`/api/airlines/${airlineId}/permissions`, { signal }),
    staleTime: 60_000,
  })
}

export function useFleet(airlineId: string, enabled: boolean) {
  return useQuery({
    queryKey: airlineKeys.fleet(airlineId),
    queryFn: ({ signal }) => api.get<Aircraft[]>('/api/aircrafts', { params: { airlineId }, signal }),
    staleTime: 5 * 60_000,
    enabled,
  })
}

// ---------------------------------------------------------------- flights (routes)

export interface FlightInput {
  flightNumber: string
  departureAirportId: string
  arrivalAirportId: string
}

export function useFlights(airlineId: string, page: number, enabled = true) {
  return useQuery({
    queryKey: [...airlineKeys.flights(airlineId), page],
    queryFn: async ({ signal }) =>
      toPageResult(
        await api.get<Page<Flight>>('/api/flights/airline', {
          params: { airlineId, page, size: 25, sort: 'flightNumber,asc' },
          signal,
        }),
      ),
    placeholderData: keepPreviousData,
    enabled,
  })
}

/** Every flight of the airline, for pickers. Airlines with more than 500 routes would need a search instead. */
export function useAllFlights(airlineId: string, enabled = true) {
  return useQuery({
    queryKey: [...airlineKeys.flights(airlineId), 'all'],
    queryFn: async ({ signal }) =>
      (
        await api.get<Page<Flight>>('/api/flights/airline', {
          params: { airlineId, page: 0, size: 500, sort: 'flightNumber,asc' },
          signal,
        })
      ).content,
    staleTime: 60_000,
    enabled,
  })
}

export function useSaveFlight(airlineId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, input }: { id?: string; input: FlightInput }) =>
      id
        ? api.put<Flight>(`/api/flights/${id}`, { ...input, airlineId })
        : api.post<Flight>('/api/flights', { ...input, airlineId }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: airlineKeys.flights(airlineId) }),
  })
}

export function useDeleteFlight(airlineId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api.delete(`/api/flights/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: airlineKeys.flights(airlineId) }),
  })
}

// ---------------------------------------------------------------- schedules

export interface ScheduleInput {
  flightId: string
  aircraftId: string
  departureTime: string
  arrivalTime: string
  startDate: string
  endDate: string
  recurrenceType: RecurrenceType
  operatingDays: DayOfWeek[]
  isActive: boolean
}

export function useSchedules(airlineId: string, enabled = true) {
  return useQuery({
    queryKey: airlineKeys.schedules(airlineId),
    queryFn: ({ signal }) => api.get<Schedule[]>('/api/flight-schedules', { params: { airlineId }, signal }),
    enabled,
  })
}

/** A schedule change regenerates departures, so both lists are refreshed. */
function useScheduleInvalidation(airlineId: string) {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: airlineKeys.schedules(airlineId) }),
      queryClient.invalidateQueries({ queryKey: airlineKeys.departures(airlineId) }),
    ])
}

export function useCreateSchedule(airlineId: string) {
  const invalidate = useScheduleInvalidation(airlineId)
  return useMutation({
    mutationFn: (input: ScheduleInput) => api.post<Schedule>('/api/flight-schedules', input),
    onSuccess: invalidate,
  })
}

export function useUpdateSchedule(airlineId: string) {
  const invalidate = useScheduleInvalidation(airlineId)
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: ScheduleInput }) =>
      api.put<ScheduleChange>(`/api/flight-schedules/${id}`, input),
    onSuccess: invalidate,
  })
}

export function useDeleteSchedule(airlineId: string) {
  const invalidate = useScheduleInvalidation(airlineId)
  return useMutation({
    mutationFn: (id: string) => api.delete<ScheduleChange>(`/api/flight-schedules/${id}`),
    onSuccess: invalidate,
  })
}

// ---------------------------------------------------------------- departures

export interface DepartureFilters {
  onDate?: string
  flightId?: string
}

export interface NewDepartureInput {
  flightId: string
  aircraftId: string
  departureDateTime: string
  arrivalDateTime: string
  /** Required by the API's DTO; the server replaces it with the aircraft's real seat count. */
  totalSeats: number
}

export interface DepartureUpdateInput {
  departureDateTime: string
  arrivalDateTime: string
  terminal: string
  gate: string
  minAdvanceBookingDays: number | null
  maxAdvanceBookingDays: number | null
}

export function useDepartures(airlineId: string, filters: DepartureFilters, page: number, enabled = true) {
  return useQuery({
    queryKey: [...airlineKeys.departures(airlineId), filters, page],
    queryFn: async ({ signal }) =>
      toPageResult(
        await api.get<Page<Departure>>('/api/flight-instances', {
          params: {
            airlineId,
            onDate: filters.onDate || undefined,
            flightId: filters.flightId || undefined,
            page,
            size: 25,
            sort: 'departureDateTime,asc',
          },
          signal,
        }),
      ),
    placeholderData: keepPreviousData,
    enabled,
  })
}

function useDepartureInvalidation(airlineId: string) {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: airlineKeys.departures(airlineId) })
}

export function useCreateDeparture(airlineId: string) {
  const invalidate = useDepartureInvalidation(airlineId)
  return useMutation({
    mutationFn: (input: NewDepartureInput) => api.post<Departure>('/api/flight-instances', input),
    onSuccess: invalidate,
  })
}

export function useUpdateDeparture(airlineId: string) {
  const invalidate = useDepartureInvalidation(airlineId)
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: DepartureUpdateInput }) =>
      api.put<Departure>(`/api/flight-instances/${id}`, input),
    onSuccess: invalidate,
  })
}

export function useChangeDepartureStatus(airlineId: string) {
  const invalidate = useDepartureInvalidation(airlineId)
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: FlightStatus }) =>
      api.patch<Departure>(`/api/flight-instances/${id}/status`, undefined, { params: { status } }),
    onSuccess: invalidate,
  })
}

export function useReassignAircraft(airlineId: string) {
  const invalidate = useDepartureInvalidation(airlineId)
  return useMutation({
    mutationFn: ({ id, aircraftId }: { id: string; aircraftId: string }) =>
      api.patch<Departure>(`/api/flight-instances/${id}/aircraft`, { aircraftId }),
    onSuccess: invalidate,
  })
}

export function useDeleteDeparture(airlineId: string) {
  const invalidate = useDepartureInvalidation(airlineId)
  return useMutation({
    mutationFn: (id: string) => api.delete(`/api/flight-instances/${id}`),
    onSuccess: invalidate,
  })
}
