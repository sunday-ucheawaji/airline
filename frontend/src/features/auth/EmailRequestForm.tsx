import { useState, type ReactNode } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button } from '@/components/ui/button'
import type { ApiError } from '@/lib/api'
import { applyServerErrors } from '@/lib/forms'
import { Field } from '@/components/form/Field'
import { emailOnlySchema, type EmailOnlyInput, type EmailOnlyValues } from './authSchemas'
import { FormAlert } from '@/components/form/FormAlert'

type EmailRequestFormProps = {
  id: string
  submitLabel: string
  pendingLabel: string
  send: (email: string, callbacks: { onSuccess: () => void; onError: (error: unknown) => void }) => void
  isPending: boolean
  /** What to show once sent; it gets the address so the message can name it. */
  sent: (email: string) => ReactNode
}

/** One email field that asks the server to send something, then says it's on its way whatever the address. */
export function EmailRequestForm({ id, submitLabel, pendingLabel, send, isPending, sent }: EmailRequestFormProps) {
  const [sentTo, setSentTo] = useState<string | null>(null)
  const [problem, setProblem] = useState<ApiError | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<EmailOnlyInput, unknown, EmailOnlyValues>({
    resolver: zodResolver(emailOnlySchema),
    defaultValues: { email: '' },
  })

  const onSubmit = handleSubmit(({ email }) => {
    setProblem(null)
    send(email, {
      onSuccess: () => setSentTo(email),
      onError: (error) => setProblem(applyServerErrors(error, setError, ['email'])),
    })
  })

  if (sentTo) return <>{sent(sentTo)}</>

  return (
    <form noValidate onSubmit={onSubmit} className="grid gap-5">
      {problem && (
        <FormAlert tone="error" title={problem.status === 429 ? 'Too many attempts' : 'We couldn’t send the email'}>
          {problem.message}
        </FormAlert>
      )}
      <Field
        id={id}
        label="Email address"
        type="email"
        autoComplete="email"
        inputMode="email"
        error={errors.email?.message}
        {...register('email')}
      />
      <Button type="submit" size="lg" className="h-11" disabled={isPending} aria-busy={isPending}>
        {isPending ? pendingLabel : submitLabel}
      </Button>
    </form>
  )
}
