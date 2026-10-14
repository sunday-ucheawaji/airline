import { Link, useSearchParams } from 'react-router'
import { AccountAside, AuthLayout } from '@/features/auth/AuthLayout'
import { RedirectIfSignedIn } from '@/features/auth/RedirectIfSignedIn'
import { SignInForm } from '@/features/auth/SignInForm'

export function SignInPage() {
  const [params] = useSearchParams()
  const redirect = params.get('redirect')
  const signUpLink = redirect ? `/sign-up?redirect=${encodeURIComponent(redirect)}` : '/sign-up'

  return (
    <RedirectIfSignedIn>
      <AuthLayout
        title="Sign in"
        intro={
          <>
            New to Contrail?{' '}
            <Link to={signUpLink} className="rounded-sm font-medium text-chart-blue underline-offset-4 hover:underline">
              Create an account
            </Link>
          </>
        }
        aside={<AccountAside />}
      >
        <SignInForm />
      </AuthLayout>
    </RedirectIfSignedIn>
  )
}
