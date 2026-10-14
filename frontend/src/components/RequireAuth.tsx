import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useSession } from '@/lib/api'

/** Sends a signed-out visitor to sign in, and back here afterwards. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading } = useSession()
  const { pathname, search } = useLocation()

  if (isLoading) {
    return (
      <p role="status" className="mx-auto max-w-6xl px-5 pt-14 text-muted-foreground sm:px-8">
        Checking whether you’re signed in…
      </p>
    )
  }
  if (!isAuthenticated) {
    return <Navigate to={`/sign-in?redirect=${encodeURIComponent(pathname + search)}`} replace />
  }
  return children
}
