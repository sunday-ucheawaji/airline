import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useSearchParams } from 'react-router'
import { Button, buttonVariants } from '@/components/ui/button'
import { PasswordField } from '@/components/form/Field'
import { AuthLayout } from '@/features/auth/AuthLayout'
import { PASSWORD_MIN, resetPasswordSchema, type ResetPasswordInput, type ResetPasswordValues } from '@/features/auth/authSchemas'
import { FormAlert } from '@/components/form/FormAlert'
import { useResetPassword, type ApiError } from '@/lib/api'
import { applyServerErrors } from '@/lib/forms'

export function ResetPasswordPage() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const reset = useResetPassword()
  const [problem, setProblem] = useState<ApiError | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors },
  } = useForm<ResetPasswordInput, unknown, ResetPasswordValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { newPassword: '' },
  })
  const newPassword = useWatch({ control, name: 'newPassword' })

  if (!token) {
    return (
      <AuthLayout title="Choose a new password">
        <FormAlert tone="error" title="This link isn’t complete">
          Open the link straight from the email, or <Link to="/forgot-password">ask for a new one</Link>.
        </FormAlert>
      </AuthLayout>
    )
  }

  if (reset.isSuccess) {
    return (
      <AuthLayout title="Password changed">
        <div className="grid gap-6">
          <FormAlert tone="success" title="Your new password is set">
            For your security we’ve signed you out everywhere. Sign in again with your new password.
          </FormAlert>
          <Link to="/sign-in" className={buttonVariants({ size: 'lg', className: 'h-11 justify-self-start' })}>
            Sign in
          </Link>
        </div>
      </AuthLayout>
    )
  }

  const onSubmit = handleSubmit(({ newPassword: password }) => {
    setProblem(null)
    reset.mutate(
      { token, newPassword: password },
      { onError: (error) => setProblem(applyServerErrors(error, setError, ['newPassword'])) },
    )
  })

  return (
    <AuthLayout title="Choose a new password" intro="Choose a password you don’t use anywhere else.">
      <form noValidate onSubmit={onSubmit} aria-labelledby="auth-title" className="grid gap-5">
        {problem &&
          (problem.code === 'INVALID_RESET_TOKEN' ? (
            <FormAlert tone="error" title="This reset link has expired or was already used">
              Reset links work once and only for a short time. <Link to="/forgot-password">Ask for a new link</Link>.
            </FormAlert>
          ) : (
            <FormAlert tone="error" title="We couldn’t change your password">
              {problem.message}
            </FormAlert>
          ))}
        <PasswordField
          id="reset-new-password"
          label="New password"
          autoComplete="new-password"
          error={errors.newPassword?.message}
          hint={newPassword.length >= PASSWORD_MIN ? `At least ${PASSWORD_MIN} characters: done` : `At least ${PASSWORD_MIN} characters`}
          {...register('newPassword')}
        />
        <Button type="submit" size="lg" className="h-11" disabled={reset.isPending} aria-busy={reset.isPending}>
          {reset.isPending ? 'Saving…' : 'Save new password'}
        </Button>
      </form>
    </AuthLayout>
  )
}
