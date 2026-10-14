import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback } from 'react'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'
import { api } from '@/lib/api'

export interface Airport {
  id: string
  iataCode: string
  name: string
  cityName: string
  cityCode: string
  countryCode: string
  countryName: string
  timeZoneId: string | null
}

export const MIN_AIRPORT_QUERY_LENGTH = 2
const SEARCH_LIMIT = 8
const SEARCH_DEBOUNCE_MS = 200
const IATA_CODE = /^[A-Z]{3}$/

const airportKeys = {
  search: (query: string) => ['airports', 'search', query] as const,
  byCode: (code: string) => ['airports', 'code', code] as const,
}

function searchAirports(query: string, signal?: AbortSignal) {
  return api.get<Airport[]>('/api/airports/search', {
    params: { q: query, limit: SEARCH_LIMIT },
    signal,
    skipAuth: true,
  })
}

export function useAirportSearch(query: string) {
  const normalized = query.trim().toLowerCase()
  const debounced = useDebouncedValue(normalized, SEARCH_DEBOUNCE_MS)
  const search = useQuery({
    queryKey: airportKeys.search(debounced),
    queryFn: ({ signal }) => searchAirports(debounced, signal),
    enabled: debounced.length >= MIN_AIRPORT_QUERY_LENGTH,
    staleTime: 5 * 60_000,
    placeholderData: keepPreviousData,
  })
  return { ...search, isTooShort: normalized.length < MIN_AIRPORT_QUERY_LENGTH, isSettling: normalized !== debounced }
}

export function useAirport(code: string) {
  return useQuery({
    queryKey: airportKeys.byCode(code),
    queryFn: async ({ signal }) => (await searchAirports(code, signal)).find((airport) => airport.iataCode === code) ?? null,
    enabled: IATA_CODE.test(code),
    staleTime: 24 * 60 * 60_000,
  })
}

export function useRememberAirport() {
  const queryClient = useQueryClient()
  return useCallback(
    (airport: Airport) => queryClient.setQueryData(airportKeys.byCode(airport.iataCode), airport),
    [queryClient],
  )
}
