import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

// Booking references are BK + 8 characters; a round trip or multi-city trip is TR + 8.
const manageBookingSchema = z.object({
  reference: z
    .string()
    .trim()
    .toUpperCase()
    .regex(/^(BK|TR)[0-9A-F]{8}$/, 'Enter the 10-character reference from your confirmation email, like BK1A2B3C4D'),
  lastName: z.string().trim().min(1, 'Enter the last name of a traveller on the booking'),
})

type ManageBookingInput = z.input<typeof manageBookingSchema>
type ManageBooking = z.output<typeof manageBookingSchema>

export function ManageBookingForm() {
  const [lookedUp, setLookedUp] = useState<ManageBooking | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ManageBookingInput, unknown, ManageBooking>({
    resolver: zodResolver(manageBookingSchema),
    defaultValues: { reference: '', lastName: '' },
  })

  return (
    <form noValidate aria-label="Find a booking" onSubmit={handleSubmit(setLookedUp)}>
      <div className="grid gap-4 sm:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_auto] sm:items-start">
        <div className="grid gap-2">
          <Label htmlFor="manage-reference">Booking reference</Label>
          <Input
            id="manage-reference"
            autoComplete="off"
            autoCapitalize="characters"
            spellCheck={false}
            placeholder="BK1A2B3C4D"
            className="h-11 bg-card uppercase placeholder:normal-case"
            aria-invalid={errors.reference ? true : undefined}
            aria-describedby={errors.reference ? 'manage-reference-error' : undefined}
            {...register('reference')}
          />
          {errors.reference && (
            <p id="manage-reference-error" className="text-sm text-destructive">
              {errors.reference.message}
            </p>
          )}
        </div>
        <div className="grid gap-2">
          <Label htmlFor="manage-last-name">Last name</Label>
          <Input
            id="manage-last-name"
            autoComplete="family-name"
            className="h-11 bg-card"
            aria-invalid={errors.lastName ? true : undefined}
            aria-describedby={errors.lastName ? 'manage-last-name-error' : undefined}
            {...register('lastName')}
          />
          {errors.lastName && (
            <p id="manage-last-name-error" className="text-sm text-destructive">
              {errors.lastName.message}
            </p>
          )}
        </div>
        <Button
          type="submit"
          variant="outline"
          className="h-11 border-chart-blue text-chart-blue hover:bg-accent hover:text-chart-blue sm:mt-[1.375rem]"
        >
          Find booking
        </Button>
      </div>
      <p role="status" className="mt-3 min-h-5 text-sm text-muted-foreground">
        {lookedUp && `Booking lookup isn’t connected yet. When it is, ${lookedUp.reference} will open here.`}
      </p>
    </form>
  )
}
