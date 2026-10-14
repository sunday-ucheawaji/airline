import { useState } from 'react'
import { format } from 'date-fns'
import { CalendarIcon } from 'lucide-react'
import { Calendar } from '@/components/ui/calendar'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { cn } from '@/lib/utils'

type DateFieldProps = {
  id: string
  label: string
  value?: Date
  onChange: (date: Date | undefined) => void
  minDate: Date
  error?: string
}

export function DateField({ id, label, value, onChange, minDate, error }: DateFieldProps) {
  const [open, setOpen] = useState(false)
  const errorId = `${id}-error`

  return (
    <div className="min-w-0">
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger
          id={id}
          aria-label={value ? `${label}: ${format(value, 'EEEE d MMMM yyyy')}` : `${label}: choose a date`}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          className={cn(
            'flex w-full items-center justify-between gap-3 rounded-md border bg-card px-3 py-2.5 text-left transition-colors hover:border-chart-blue',
            error && 'border-destructive',
          )}
        >
          <span className="min-w-0">
            <span className="block text-sm font-medium text-muted-foreground">{label}</span>
            <span className={cn('block text-lg font-semibold', !value && 'font-normal text-muted-foreground')}>
              {value ? format(value, 'EEE d MMM') : 'Add date'}
            </span>
          </span>
          <CalendarIcon aria-hidden className="size-5 shrink-0 text-chart-blue" />
        </PopoverTrigger>
        <PopoverContent align="start" className="w-auto p-0">
          <Calendar
            mode="single"
            selected={value}
            defaultMonth={value ?? minDate}
            disabled={{ before: minDate }}
            onSelect={(date) => {
              onChange(date)
              setOpen(false)
            }}
          />
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
