import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import { useLogin, useResendVerification, type ApiError } from '@/lib/api'
import { applyServerErrors } from '@/lib/forms'
import { Field, PasswordField } from '@/components/form/Field'
import { signInSchema, type SignInInput, type SignInValues } from './authSchemas'
import { FormAlert } from '@/components/form/FormAlert'

/** On success the session changes, and the page's RedirectIfSignedIn moves on to where the user was going. */
export function SignInForm() {
  const login = useLogin()
  const [problem, setProblem] = useState<{ error: ApiError; email: string } | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<SignInInput, unknown, SignInValues>({
    resolver: zodResolver(signInSchema),
    defaultValues: { email: '', password: '' },
  })

  const onSubmit = handleSubmit((values) => {
    setProblem(null)
    login.mutate(values, {
      onError: (error) => {
        const left = applyServerErrors(error, setError, ['email', 'password'])
        if (left) setProblem({ error: left, email: values.email })
      },
    })
  })

  return (
    <form noValidate onSubmit={onSubmit} aria-labelledby="auth-title" className="grid gap-5">
      {problem && <SignInProblem error={problem.error} email={problem.email} />}

      <Field
        id="sign-in-email"
        label="Email address"
        type="email"
        autoComplete="username"
        inputMode="email"
        error={errors.email?.message}
        {...register('email')}
      />
      <div className="grid gap-2">
        <PasswordField
          id="sign-in-password"
          label="Password"
          autoComplete="current-password"
          error={errors.password?.message}
          {...register('password')}
        />
        <Link to="/forgot-password" className="justify-self-start rounded-sm text-sm font-medium text-chart-blue underline-offset-4 hover:underline">
          Forgot your password?
        </Link>
      </div>

      <Button type="submit" size="lg" className="mt-2 h-11" disabled={login.isPending} aria-busy={login.isPending}>
        {login.isPending ? 'Signing in…' : 'Sign in'}
      </Button>
    </form>
  )
}

function SignInProblem({ error, email }: { error: ApiError; email: string }) {
  switch (error.code) {
    case 'INVALID_CREDENTIALS':
      return (
        <FormAlert tone="error" title="Email or password is incorrect">
          Check both and try again. If you’ve forgotten your password, you can{' '}
          <Link to="/forgot-password">reset it</Link>.
        </FormAlert>
      )
    case 'EMAIL_NOT_VERIFIED':
      return <UnverifiedEmailAlert email={email} />
    case 'ACCOUNT_LOCKED':
      return (
        <FormAlert tone="error" title="Your account is locked for now">
          There were too many wrong passwords. Wait a few minutes and try again, or{' '}
          <Link to="/forgot-password">reset your password</Link> to unlock it straight away.
        </FormAlert>
      )
    case 'ACCOUNT_NOT_ACTIVE':
      return (
        <FormAlert tone="error" title="This account can’t sign in">
          It has been suspended or closed. Contact support if you think this is a mistake.
        </FormAlert>
      )
  }
  if (error.status === 429) {
    return (
      <FormAlert tone="error" title="Too many attempts">
        {error.message}
      </FormAlert>
    )
  }
  if (error.kind === 'network' || error.kind === 'timeout') {
    return (
      <FormAlert tone="error" title="We couldn’t reach Contrail">
        {error.message}
      </FormAlert>
    )
  }
  return (
    <FormAlert tone="error" title="We couldn’t sign you in">
      {error.message}
    </FormAlert>
  )
}

function UnverifiedEmailAlert({ email }: { email: string }) {
  const resend = useResendVerification()

  if (resend.isSuccess) {
    return (
      <FormAlert tone="success" title="We’ve sent a new link">
        Check {email} for an email from Contrail and open the link to verify your address, then sign in.
      </FormAlert>
    )
  }
  return (
    <FormAlert tone="error" title="Verify your email address first">
      <p>We sent a link to {email} when you created your account. Open it, then sign in.</p>
      <Button
        type="button"
        variant="outline"
        size="sm"
        className="mt-3"
        disabled={resend.isPending}
        onClick={() => resend.mutate(email)}
      >
        {resend.isPending ? 'Sending…' : 'Send a new link'}
      </Button>
      {resend.isError && <p className="mt-2 text-destructive">{resend.error.message}</p>}
    </FormAlert>
  )
}
