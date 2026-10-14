import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Check } from 'lucide-react'
import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import { useResendVerification, useSignup, type ApiError } from '@/lib/api'
import { applyServerErrors } from '@/lib/forms'
import { cn } from '@/lib/utils'
import { Field, PasswordField } from '@/components/form/Field'
import { PASSWORD_MIN, signUpSchema, type SignUpInput, type SignUpValues } from './authSchemas'
import { FormAlert } from '@/components/form/FormAlert'
import { useCooldown } from './useCooldown'

const FIELDS = ['firstName', 'lastName', 'email', 'password'] as const

export function SignUpForm() {
  const signup = useSignup()
  const [sentTo, setSentTo] = useState<string | null>(null)
  const [problem, setProblem] = useState<ApiError | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors },
  } = useForm<SignUpInput, unknown, SignUpValues>({
    resolver: zodResolver(signUpSchema),
    defaultValues: { firstName: '', lastName: '', email: '', password: '' },
  })
  const password = useWatch({ control, name: 'password' })

  const onSubmit = handleSubmit((values) => {
    setProblem(null)
    signup.mutate(values, {
      onSuccess: () => setSentTo(values.email),
      onError: (error) => setProblem(applyServerErrors(error, setError, FIELDS)),
    })
  })

  if (sentTo) return <CheckYourEmail email={sentTo} onUseAnotherEmail={() => setSentTo(null)} />

  return (
    <form noValidate onSubmit={onSubmit} aria-labelledby="auth-title" className="grid gap-5">
      {problem && (
        <FormAlert tone="error" title={problem.status === 429 ? 'Too many attempts' : 'We couldn’t create your account'}>
          {problem.message}
        </FormAlert>
      )}

      <div className="grid gap-5 sm:grid-cols-2">
        <Field
          id="sign-up-first-name"
          label="First name"
          autoComplete="given-name"
          error={errors.firstName?.message}
          {...register('firstName')}
        />
        <Field
          id="sign-up-last-name"
          label="Last name"
          autoComplete="family-name"
          error={errors.lastName?.message}
          {...register('lastName')}
        />
      </div>
      <Field
        id="sign-up-email"
        label="Email address"
        type="email"
        autoComplete="email"
        inputMode="email"
        error={errors.email?.message}
        hint="We’ll send a link to this address to confirm it’s yours."
        {...register('email')}
      />
      <PasswordField
        id="sign-up-password"
        label="Password"
        autoComplete="new-password"
        error={errors.password?.message}
        hint={<PasswordRequirement met={password.length >= PASSWORD_MIN} />}
        {...register('password')}
      />

      <Button type="submit" size="lg" className="mt-2 h-11" disabled={signup.isPending} aria-busy={signup.isPending}>
        {signup.isPending ? 'Creating your account…' : 'Create account'}
      </Button>
      <p className="text-sm text-muted-foreground">
        Already have an account?{' '}
        <Link to="/sign-in" className="rounded-sm font-medium text-chart-blue underline-offset-4 hover:underline">
          Sign in
        </Link>
      </p>
    </form>
  )
}

function PasswordRequirement({ met }: { met: boolean }) {
  return (
    <span className={cn('inline-flex items-center gap-1.5', met && 'text-chart-blue')}>
      <Check aria-hidden className={cn('size-3.5', !met && 'opacity-30')} />
      At least {PASSWORD_MIN} characters
      <span className="sr-only">{met ? ', done' : ', not yet'}</span>
    </span>
  )
}

const RESEND_COOLDOWN_SECONDS = 60

/** Worded so it never says whether the address already had an account; the server answers the same way. */
function CheckYourEmail({ email, onUseAnotherEmail }: { email: string; onUseAnotherEmail: () => void }) {
  const resend = useResendVerification()
  const cooldown = useCooldown(RESEND_COOLDOWN_SECONDS)

  const sendAgain = () =>
    resend.mutate(email, {
      onSuccess: cooldown.start,
    })

  return (
    <div className="grid gap-6">
      <FormAlert tone="success" title="Check your email">
        If <span className="font-medium text-foreground">{email}</span> can be used for a new account, we’ve sent it a
        link to verify the address. Open the link, then sign in.
      </FormAlert>
      {resend.isError && (
        <FormAlert tone="error" title="We couldn’t send another email">
          {resend.error.message}
        </FormAlert>
      )}
      {resend.isSuccess && cooldown.isCoolingDown && <p role="status" className="text-sm text-muted-foreground">Sent again.</p>}
      <div className="flex flex-wrap items-center gap-3">
        <Button type="button" variant="outline" onClick={sendAgain} disabled={resend.isPending || cooldown.isCoolingDown}>
          {resend.isPending
            ? 'Sending…'
            : cooldown.isCoolingDown
              ? `Send again in ${cooldown.remaining}s`
              : 'Send the email again'}
        </Button>
        <Button type="button" variant="ghost" onClick={onUseAnotherEmail}>
          Use a different email
        </Button>
      </div>
      <p className="text-sm text-muted-foreground">
        Verified already?{' '}
        <Link to="/sign-in" className="rounded-sm font-medium text-chart-blue underline-offset-4 hover:underline">
          Sign in
        </Link>
      </p>
    </div>
  )
}
