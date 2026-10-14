import { AccountAside, AuthLayout } from '@/features/auth/AuthLayout'
import { RedirectIfSignedIn } from '@/features/auth/RedirectIfSignedIn'
import { SignUpForm } from '@/features/auth/SignUpForm'

export function SignUpPage() {
  return (
    <RedirectIfSignedIn>
      <AuthLayout
        title="Create an account"
        intro="Keep your bookings in one place and manage them whenever you need to."
        aside={<AccountAside />}
      >
        <SignUpForm />
      </AuthLayout>
    </RedirectIfSignedIn>
  )
}
