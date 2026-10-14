import { QueryClient } from '@tanstack/react-query'
import { isRetryable, type ApiError } from './errors'
import { getSnapshot, subscribe } from './session'

declare module '@tanstack/react-query' {
  interface Register {
    defaultError: ApiError
  }
}

const MAX_RETRIES = 2

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      gcTime: 5 * 60_000,
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => failureCount < MAX_RETRIES && isRetryable(error),
      retryDelay: (attempt) => Math.min(1_000 * 2 ** attempt, 8_000),
    },
    mutations: {
      retry: false,
    },
  },
})

let currentUserId = getSnapshot().user?.id
subscribe(() => {
  const nextUserId = getSnapshot().user?.id
  if (currentUserId && nextUserId !== currentUserId) queryClient.clear()
  currentUserId = nextUserId
})
