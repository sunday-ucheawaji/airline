import { useState } from 'react'
import { MinusIcon, PlusIcon, UsersIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { cn } from '@/lib/utils'
import { describeTravellers } from './searchParams'
import { CABINS, CABIN_LABELS, MAX_SEATED_TRAVELLERS, type Cabin } from './searchSchema'

export type Travellers = {
  adults: number
  children: number
  infants: number
  cabin: Cabin
}

type TravellersFieldProps = {
  id: string
  value: Travellers
  onChange: (value: Travellers) => void
  error?: string
}

type StepperProps = {
  label: string
  hint: string
  singular: string
  value: number
  min: number
  max: number
  onChange: (value: number) => void
}

function Stepper({ label, hint, singular, value, min, max, onChange }: StepperProps) {
  return (
    <div className="flex items-center justify-between gap-4 py-3">
      <div>
        <p className="font-medium">{label}</p>
        <p className="text-sm text-muted-foreground">{hint}</p>
      </div>
      <div className="flex items-center gap-3">
        <Button
          type="button"
          variant="outline"
          size="icon"
          className="rounded-full"
          aria-label={`Remove ${singular}`}
          disabled={value <= min}
          onClick={() => onChange(value - 1)}
        >
          <MinusIcon aria-hidden />
        </Button>
        <span aria-live="polite" className="w-5 text-center text-lg font-semibold tabular-nums">
          {value}
        </span>
        <Button
          type="button"
          variant="outline"
          size="icon"
          className="rounded-full"
          aria-label={`Add ${singular}`}
          disabled={value >= max}
          onClick={() => onChange(value + 1)}
        >
          <PlusIcon aria-hidden />
        </Button>
      </div>
    </div>
  )
}

export function TravellersField({ id, value, onChange, error }: TravellersFieldProps) {
  const [open, setOpen] = useState(false)
  const { adults, children, infants, cabin } = value
  const summary = `${describeTravellers(value)}, ${CABIN_LABELS[cabin]}`
  const errorId = `${id}-error`

  const setAdults = (next: number) => onChange({ ...value, adults: next, infants: Math.min(infants, next) })

  return (
    <div className="min-w-0">
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger
          id={id}
          aria-label={`Travellers and cabin: ${summary}`}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          className={cn(
            'flex w-full items-center justify-between gap-3 rounded-md border bg-card px-3 py-2.5 text-left transition-colors hover:border-chart-blue',
            error && 'border-destructive',
          )}
        >
          <span className="min-w-0">
            <span className="block text-sm font-medium text-muted-foreground">Travellers and cabin</span>
            <span className="block truncate text-lg font-semibold">{summary}</span>
          </span>
          <UsersIcon aria-hidden className="size-5 shrink-0 text-chart-blue" />
        </PopoverTrigger>
        <PopoverContent align="end" className="w-[min(22rem,calc(100vw-2rem))]">
          <div className="divide-y">
            <Stepper
              label="Adults"
              hint="12 and over"
              singular="adult"
              value={adults}
              min={1}
              max={MAX_SEATED_TRAVELLERS - children}
              onChange={setAdults}
            />
            <Stepper
              label="Children"
              hint="2 to 11"
              singular="child"
              value={children}
              min={0}
              max={MAX_SEATED_TRAVELLERS - adults}
              onChange={(next) => onChange({ ...value, children: next })}
            />
            <Stepper
              label="Infants"
              hint="Under 2, on an adult’s lap"
              singular="infant"
              value={infants}
              min={0}
              max={adults}
              onChange={(next) => onChange({ ...value, infants: next })}
            />
          </div>
          <div className="mt-3 grid gap-2">
            <Label htmlFor={`${id}-cabin`}>Cabin</Label>
            <Select value={cabin} onValueChange={(next) => onChange({ ...value, cabin: next as Cabin })}>
              <SelectTrigger id={`${id}-cabin`} className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {CABINS.map((option) => (
                  <SelectItem key={option} value={option}>
                    {CABIN_LABELS[option]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <Button type="button" variant="outline" className="mt-4 w-full" onClick={() => setOpen(false)}>
            Done
          </Button>
        </PopoverContent>
      </Popover>
      {error && (
        <p id={errorId} className="mt-2 text-sm text-destructive">
          {error}
        </p>
      )}
    </div>
  )
}
