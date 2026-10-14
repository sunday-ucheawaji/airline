import type { ReactNode } from 'react'
import { Navigate, useSearchParams } from 'react-router'
import { useSession } from '@/lib/api'
import { safeRedirect } from './authSchemas'

/** Sign-in and sign-up pages send someone already signed in on to where they were going. */
export function RedirectIfSignedIn({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading } = useSession()
  const [params] = useSearchParams()

  if (isLoading) {
    return (
      <p role="status" className="mx-auto max-w-6xl px-5 pt-14 text-muted-foreground sm:px-8">
        Checking whether you’re signed in…
      </p>
    )
  }
  if (isAuthenticated) return <Navigate to={safeRedirect(params.get('redirect'))} replace />
  return children
}
