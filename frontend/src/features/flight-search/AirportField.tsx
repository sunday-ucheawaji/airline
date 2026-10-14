import { useState } from 'react'
import { Command, CommandInput, CommandItem, CommandList } from '@/components/ui/command'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { cn } from '@/lib/utils'
import { MIN_AIRPORT_QUERY_LENGTH, useAirport, useAirportSearch, useRememberAirport } from './airports'

type AirportFieldProps = {
  id: string
  label: string
  value: string
  onChange: (code: string) => void
  error?: string
  size?: 'hero' | 'compact'
  codeClassName?: string
}

export function AirportField({ id, label, value, onChange, error, size = 'hero', codeClassName }: AirportFieldProps) {
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const { data: airport } = useAirport(value)
  const search = useAirportSearch(query)
  const rememberAirport = useRememberAirport()
  const errorId = `${id}-error`
  const results = search.isTooShort ? [] : (search.data ?? [])
  const status = searchStatus(search, results.length)

  const changeOpen = (next: boolean) => {
    setOpen(next)
    if (!next) setQuery('')
  }

  return (
    <div className="min-w-0">
      <Popover open={open} onOpenChange={changeOpen}>
        <PopoverTrigger
          id={id}
          aria-label={
            airport
              ? `${label}: ${airport.cityName}, ${airport.name} (${airport.iataCode})`
              : value
                ? `${label}: ${value}`
                : `${label}: choose an airport`
          }
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          className="group block w-full min-w-0 rounded-md text-left"
        >
          <span className="block text-sm font-medium text-muted-foreground">{label}</span>
          <span
            className={cn(
              'block font-display transition-colors',
              size === 'hero' ? 'text-[clamp(4.5rem,11vw,7.5rem)]' : 'text-5xl',
              value ? 'text-foreground' : 'text-border group-hover:text-muted-foreground',
              codeClassName,
            )}
          >
            {value ? (
              value
            ) : (
              <span aria-hidden className="inline-flex gap-[0.08em] py-[0.04em]">
                {[0, 1, 2].map((box) => (
                  <span
                    key={box}
                    className="inline-block h-[0.84em] w-[0.5em] rounded-[0.06em] border-2 border-dashed border-current"
                  />
                ))}
              </span>
            )}
          </span>
          <span className={cn('block truncate', size === 'hero' ? 'mt-2 text-base' : 'mt-1 text-sm')}>
            {airport ? (
              <>
                {airport.cityName}
                <span className="text-muted-foreground">, {airport.name}</span>
              </>
            ) : value ? (
              <span className="text-muted-foreground">Loading airport…</span>
            ) : (
              <span className="text-muted-foreground">City or airport</span>
            )}
          </span>
        </PopoverTrigger>
        <PopoverContent align="start" className="w-[min(22rem,calc(100vw-2rem))] p-0">
          <Command shouldFilter={false}>
            <CommandInput placeholder="City, airport or code" value={query} onValueChange={setQuery} />
            <CommandList aria-busy={search.isFetching || search.isSettling}>
              {status && (
                <p role="status" className="px-3 py-6 text-center text-sm text-muted-foreground">
                  {status}
                  {search.isError && !search.isTooShort && (
                    <button
                      type="button"
                      onClick={() => search.refetch()}
                      className="mt-2 block w-full font-medium text-chart-blue underline underline-offset-4"
                    >
                      Try again
                    </button>
                  )}
                </p>
              )}
              {results.map((option) => (
                <CommandItem
                  key={option.iataCode}
                  value={option.iataCode}
                  onSelect={() => {
                    rememberAirport(option)
                    onChange(option.iataCode)
                    changeOpen(false)
                  }}
                  className="gap-3"
                >
                  <span className="w-10 font-display text-xl">{option.iataCode}</span>
                  <span className="min-w-0">
                    <span className="block truncate">
                      {option.cityName}, {option.countryName}
                    </span>
                    <span className="block truncate text-xs text-muted-foreground">{option.name}</span>
                  </span>
                </CommandItem>
              ))}
            </CommandList>
          </Command>
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

function searchStatus(search: ReturnType<typeof useAirportSearch>, resultCount: number) {
  if (search.isTooShort) return `Type at least ${MIN_AIRPORT_QUERY_LENGTH} letters of a city, airport or code.`
  if (search.isError) return search.error.message
  if (resultCount === 0 && (search.isSettling || search.isFetching)) return 'Searching…'
  if (resultCount === 0) return 'No airports match that search.'
  return null
}
