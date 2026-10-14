import { Link } from 'react-router'
import { AuthLayout } from '@/features/auth/AuthLayout'
import { EmailRequestForm } from '@/features/auth/EmailRequestForm'
import { FormAlert } from '@/components/form/FormAlert'
import { useRequestPasswordReset } from '@/lib/api'

export function ForgotPasswordPage() {
  const reset = useRequestPasswordReset()

  return (
    <AuthLayout
      title="Reset your password"
      intro="Enter the email address you sign in with and we’ll send you a link to choose a new password."
    >
      <EmailRequestForm
        id="forgot-password-email"
        submitLabel="Send reset link"
        pendingLabel="Sending…"
        isPending={reset.isPending}
        send={(email, callbacks) => reset.mutate(email, callbacks)}
        sent={(email) => (
          <div className="grid gap-6">
            <FormAlert tone="success" title="Check your email">
              If <span className="font-medium text-foreground">{email}</span> belongs to an account, we’ve sent it a link
              to reset the password. The link works once and expires soon, so use it shortly.
            </FormAlert>
            <Link to="/sign-in" className="justify-self-start rounded-sm font-medium text-chart-blue underline-offset-4 hover:underline">
              Back to sign in
            </Link>
          </div>
        )}
      />
    </AuthLayout>
  )
}
