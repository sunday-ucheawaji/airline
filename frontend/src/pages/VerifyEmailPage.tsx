import { useEffect, useRef } from 'react'
import { Link, useSearchParams } from 'react-router'
import { buttonVariants } from '@/components/ui/button'
import { AuthLayout } from '@/features/auth/AuthLayout'
import { EmailRequestForm } from '@/features/auth/EmailRequestForm'
import { FormAlert } from '@/components/form/FormAlert'
import { toApiError, useResendVerification, useVerifyEmail } from '@/lib/api'

export function VerifyEmailPage() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const verify = useVerifyEmail()
  const sentFor = useRef<string | null>(null)

  // A verification link works once; StrictMode's second effect run must not spend it twice.
  useEffect(() => {
    if (!token || sentFor.current === token) return
    sentFor.current = token
    verify.mutate(token)
  }, [token, verify])

  if (!token) {
    return (
      <AuthLayout title="Verify your email" intro="This link is missing part of its address. Ask for a new one below.">
        <ResendVerification />
      </AuthLayout>
    )
  }

  if (verify.isSuccess) {
    return (
      <AuthLayout title="Email verified">
        <div className="grid gap-6">
          <FormAlert tone="success" title="You’re all set">
            Your email address is verified. You can now sign in to your account.
          </FormAlert>
          <Link to="/sign-in" className={buttonVariants({ size: 'lg', className: 'h-11 justify-self-start' })}>
            Sign in
          </Link>
        </div>
      </AuthLayout>
    )
  }

  if (verify.isError) {
    const error = toApiError(verify.error)
    const used = error.code === 'VERIFICATION_TOKEN_USED'
    const expired = error.code === 'VERIFICATION_TOKEN_EXPIRED'
    const linkProblem = used || expired || error.code === 'INVALID_VERIFICATION_TOKEN'
    return (
      <AuthLayout title="Verify your email">
        <div className="grid gap-8">
          {used ? (
            <FormAlert tone="error" title="This link has already been used">
              If you’ve verified your email, <Link to="/sign-in">sign in</Link>. Each new email replaces the link in the
              one before it, so if you asked for another, use the newest one or get a fresh link below.
            </FormAlert>
          ) : expired ? (
            <FormAlert tone="error" title="This link has expired">
              Verification links only last a limited time. Enter your email below and we’ll send a new one.
            </FormAlert>
          ) : linkProblem ? (
            <FormAlert tone="error" title="This link isn’t valid">
              It may have been copied incompletely. Open it straight from the email, or get a new one below.
            </FormAlert>
          ) : (
            <FormAlert tone="error" title="We couldn’t verify your email">
              {error.message}
            </FormAlert>
          )}
          {linkProblem ? <ResendVerification /> : <RetryButton onRetry={() => verify.mutate(token)} />}
        </div>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout title="Verify your email">
      <p role="status" className="text-muted-foreground">
        Verifying your email address…
      </p>
    </AuthLayout>
  )
}

function RetryButton({ onRetry }: { onRetry: () => void }) {
  return (
    <button type="button" onClick={onRetry} className={buttonVariants({ variant: 'outline', className: 'justify-self-start' })}>
      Try again
    </button>
  )
}

function ResendVerification() {
  const resend = useResendVerification()
  return (
    <EmailRequestForm
      id="verify-email-resend"
      submitLabel="Send a new link"
      pendingLabel="Sending…"
      isPending={resend.isPending}
      send={(email, callbacks) => resend.mutate(email, callbacks)}
      sent={(email) => (
        <FormAlert tone="success" title="Check your email">
          If <span className="font-medium text-foreground">{email}</span> has an account that still needs verifying,
          we’ve sent it a new link.
        </FormAlert>
      )}
    />
  )
}
