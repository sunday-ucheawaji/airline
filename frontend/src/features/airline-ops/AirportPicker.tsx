import { useState } from 'react'
import { ChevronsUpDown } from 'lucide-react'
import { Command, CommandInput, CommandItem, CommandList } from '@/components/ui/command'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { MIN_AIRPORT_QUERY_LENGTH, useAirportSearch } from '@/features/flight-search/airports'
import { cn } from '@/lib/utils'

export interface PickedAirport {
  id: string
  iataCode: string
  name: string
  cityName?: string
}

type AirportPickerProps = {
  id: string
  label: string
  value: PickedAirport | null
  onChange: (airport: PickedAirport) => void
  error?: string
  disabled?: boolean
  hint?: string
}

/** Search airports by city, name or code and keep the chosen airport's id, which is what the flight API needs. */
export function AirportPicker({ id, label, value, onChange, error, disabled, hint }: AirportPickerProps) {
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const search = useAirportSearch(query)
  const results = search.isTooShort ? [] : (search.data ?? [])
  const describedBy = [error && `${id}-error`, hint && `${id}-hint`].filter(Boolean).join(' ') || undefined

  const status = search.isTooShort
    ? `Type at least ${MIN_AIRPORT_QUERY_LENGTH} letters`
    : search.isError
      ? 'Airports couldn’t be loaded'
      : search.isFetching || search.isSettling
        ? 'Searching…'
        : results.length === 0
          ? 'No airport matches'
          : null

  return (
    <div className="grid gap-2">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
      </label>
      <Popover
        open={open}
        onOpenChange={(next) => {
          setOpen(next)
          if (!next) setQuery('')
        }}
      >
        <PopoverTrigger
          id={id}
          disabled={disabled}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          className={cn(
            'flex h-11 w-full items-center justify-between gap-2 rounded-md border border-input bg-card px-3 text-left text-base outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-60 aria-invalid:border-destructive md:text-sm',
            !value && 'text-muted-foreground',
          )}
        >
          <span className="truncate">
            {value ? (
              <>
                <span className="font-semibold text-foreground">{value.iataCode}</span>{' '}
                {value.cityName ? `${value.cityName}, ` : ''}
                {value.name}
              </>
            ) : (
              'Choose an airport'
            )}
          </span>
          <ChevronsUpDown aria-hidden className="size-4 shrink-0 opacity-50" />
        </PopoverTrigger>
        <PopoverContent className="w-(--radix-popover-trigger-width) min-w-72 p-0" align="start">
          <Command shouldFilter={false}>
            <CommandInput placeholder="City, airport or code" value={query} onValueChange={setQuery} />
            <CommandList>
              {status && <p className="px-3 py-4 text-sm text-muted-foreground">{status}</p>}
              {results.map((airport) => (
                <CommandItem
                  key={airport.id}
                  value={airport.id}
                  onSelect={() => {
                    onChange({ id: airport.id, iataCode: airport.iataCode, name: airport.name, cityName: airport.cityName })
                    setOpen(false)
                    setQuery('')
                  }}
                >
                  <span className="w-10 font-semibold">{airport.iataCode}</span>
                  <span className="truncate">
                    {airport.cityName}, {airport.name}
                  </span>
                </CommandItem>
              ))}
            </CommandList>
          </Command>
        </PopoverContent>
      </Popover>
      {error && (
        <p id={`${id}-error`} className="text-sm text-destructive">
          {error}
        </p>
      )}
      {hint && (
        <p id={`${id}-hint`} className="text-sm text-muted-foreground">
          {hint}
        </p>
      )}
    </div>
  )
}
