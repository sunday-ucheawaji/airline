import { useState } from 'react'
import { Controller, useFieldArray, useForm, useWatch } from 'react-hook-form'
import { useNavigate } from 'react-router'
import { zodResolver } from '@hookform/resolvers/zod'
import { startOfToday } from 'date-fns'
import { ArrowLeftRightIcon, PlusIcon, XIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { AirportField } from './AirportField'
import { DateField } from './DateField'
import { toSearchParams } from './searchParams'
import { MAX_LEGS, TRIP_TYPES, TRIP_TYPE_LABELS, searchSchema, type FlightSearch, type TripType } from './searchSchema'
import { TravellersField } from './TravellersField'

const swapAnimation = 'motion-safe:animate-[code-swap_220ms_ease-out]'

const emptyLeg = (from = ''): FlightSearch['legs'][number] => ({ from, to: '', date: undefined })

export function FlightSearchForm() {
  const navigate = useNavigate()
  const today = startOfToday()
  const [swaps, setSwaps] = useState<Record<number, number>>({})

  const {
    control,
    handleSubmit,
    getValues,
    setValue,
    formState: { errors, isSubmitted },
  } = useForm<FlightSearch>({
    resolver: zodResolver(searchSchema),
    defaultValues: {
      tripType: 'return',
      legs: [emptyLeg()],
      returnDate: undefined,
      adults: 1,
      children: 0,
      infants: 0,
      cabin: 'ECONOMY',
    },
  })
  const { fields, append, remove, replace } = useFieldArray({ control, name: 'legs' })
  const tripType = useWatch({ control, name: 'tripType' })
  const legs = useWatch({ control, name: 'legs' })
  const [adults, children, infants, cabin] = useWatch({ control, name: ['adults', 'children', 'infants', 'cabin'] })
  const isMultiCity = tripType === 'multi-city'

  const changeTripType = (next: TripType) => {
    const current = getValues('legs')
    if (next === 'multi-city' && current.length < 2) {
      append(emptyLeg(current[0]?.to))
    } else if (next !== 'multi-city' && current.length > 1) {
      replace([current[0]])
    }
    setValue('tripType', next)
  }

  const swap = (index: number) => {
    const { from, to } = getValues(`legs.${index}`)
    setValue(`legs.${index}.from`, to, { shouldValidate: isSubmitted })
    setValue(`legs.${index}.to`, from, { shouldValidate: isSubmitted })
    setSwaps((previous) => ({ ...previous, [index]: (previous[index] ?? 0) + 1 }))
  }

  const onSubmit = (search: FlightSearch) => {
    navigate(`/flights/search?${toSearchParams(search)}`)
  }

  const legErrors = errors.legs
  const travellerError = errors.children?.message ?? errors.infants?.message

  return (
    <form
      noValidate
      aria-label="Search flights"
      onSubmit={handleSubmit(onSubmit)}
      className="relative grid overflow-hidden rounded-2xl bg-card shadow-[0_0_0_1px_var(--rule),0_30px_60px_-30px_rgb(19_33_58/0.35)] md:grid-cols-[minmax(0,1fr)_20rem]"
    >
      <div className="flex flex-col p-5 sm:p-8">
        <fieldset>
          <legend className="sr-only">Trip type</legend>
          <div className="flex flex-wrap gap-2">
            {TRIP_TYPES.map((option) => (
              <label
                key={option}
                className="cursor-pointer rounded-full border px-4 py-1.5 text-sm font-medium transition-colors hover:border-foreground has-checked:border-foreground has-checked:bg-foreground has-checked:text-background has-focus-visible:outline-2 has-focus-visible:outline-offset-2 has-focus-visible:outline-ring"
              >
                <input
                  type="radio"
                  name="tripType"
                  value={option}
                  checked={tripType === option}
                  onChange={() => changeTripType(option)}
                  className="sr-only"
                />
                {TRIP_TYPE_LABELS[option]}
              </label>
            ))}
          </div>
        </fieldset>

        {!isMultiCity ? (
          <div className="mt-6 grid items-center gap-x-4 gap-y-2 md:my-auto md:pt-4 md:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)]">
            <Controller
              control={control}
              name="legs.0.from"
              render={({ field }) => (
                <AirportField
                  key={`from-${swaps[0] ?? 0}`}
                  id="search-from"
                  label="From"
                  value={field.value}
                  onChange={field.onChange}
                  error={legErrors?.[0]?.from?.message}
                  codeClassName={cn(
                    '[--swap-dy:45%] md:[--swap-dx:70%] md:[--swap-dy:0]',
                    swaps[0] && swapAnimation,
                  )}
                />
              )}
            />
            <div className="flex items-center justify-start md:justify-center">
              <span aria-hidden className="hidden h-px w-6 bg-chart-blue/40 md:block" />
              <Button
                type="button"
                variant="outline"
                size="icon"
                aria-label="Swap origin and destination"
                onClick={() => swap(0)}
                className="size-11 rounded-full border-chart-blue/40 text-chart-blue hover:bg-accent hover:text-chart-blue"
              >
                <ArrowLeftRightIcon aria-hidden className="rotate-90 md:rotate-0" />
              </Button>
              <span aria-hidden className="hidden h-px w-6 bg-chart-blue/40 md:block" />
            </div>
            <Controller
              control={control}
              name="legs.0.to"
              render={({ field }) => (
                <AirportField
                  key={`to-${swaps[0] ?? 0}`}
                  id="search-to"
                  label="To"
                  value={field.value}
                  onChange={field.onChange}
                  error={legErrors?.[0]?.to?.message}
                  codeClassName={cn(
                    '[--swap-dy:-45%] md:[--swap-dx:-70%] md:[--swap-dy:0]',
                    swaps[0] && swapAnimation,
                  )}
                />
              )}
            />
          </div>
        ) : (
          <div className="mt-6">
            <ol className="divide-y">
              {fields.map((field, index) => (
                <li
                  key={field.id}
                  className="grid items-start gap-x-4 gap-y-3 py-4 first:pt-0 sm:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] lg:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)_11rem_2.5rem]"
                >
                  <h3 className="sr-only">Flight {index + 1}</h3>
                  <Controller
                    control={control}
                    name={`legs.${index}.from`}
                    render={({ field: from }) => (
                      <AirportField
                        id={`leg-${index}-from`}
                        label={`Flight ${index + 1} from`}
                        size="compact"
                        value={from.value}
                        onChange={from.onChange}
                        error={legErrors?.[index]?.from?.message}
                      />
                    )}
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    aria-label={`Swap origin and destination for flight ${index + 1}`}
                    onClick={() => swap(index)}
                    className="text-chart-blue hover:text-chart-blue sm:mt-9"
                  >
                    <ArrowLeftRightIcon aria-hidden className="rotate-90 sm:rotate-0" />
                  </Button>
                  <Controller
                    control={control}
                    name={`legs.${index}.to`}
                    render={({ field: to }) => (
                      <AirportField
                        id={`leg-${index}-to`}
                        label="To"
                        size="compact"
                        value={to.value}
                        onChange={(code) => {
                          to.onChange(code)
                          if (index + 1 < getValues('legs').length && !getValues(`legs.${index + 1}.from`)) {
                            setValue(`legs.${index + 1}.from`, code)
                          }
                        }}
                        error={legErrors?.[index]?.to?.message}
                      />
                    )}
                  />
                  <Controller
                    control={control}
                    name={`legs.${index}.date`}
                    render={({ field: date }) => (
                      <div className="lg:mt-5">
                        <DateField
                          id={`leg-${index}-date`}
                          label="Depart"
                          value={date.value}
                          onChange={date.onChange}
                          minDate={legs[index - 1]?.date ?? today}
                          error={legErrors?.[index]?.date?.message}
                        />
                      </div>
                    )}
                  />
                  {index >= 2 ? (
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      aria-label={`Remove flight ${index + 1}`}
                      onClick={() => remove(index)}
                      className="text-muted-foreground lg:mt-7"
                    >
                      <XIcon aria-hidden />
                    </Button>
                  ) : (
                    <span aria-hidden className="hidden lg:block" />
                  )}
                </li>
              ))}
            </ol>
            {fields.length < MAX_LEGS && (
              <Button
                type="button"
                variant="ghost"
                onClick={() => append(emptyLeg(legs[legs.length - 1]?.to))}
                className="mt-2 -ml-3 text-chart-blue hover:text-chart-blue"
              >
                <PlusIcon aria-hidden />
                Add another flight
              </Button>
            )}
          </div>
        )}
      </div>

      <div className="relative flex flex-col gap-4 border-t-2 border-dashed p-5 sm:p-8 md:border-t-0 md:border-l-2">
        <span aria-hidden className="absolute -top-3 -left-3 size-6 rounded-full bg-background" />
        <span aria-hidden className="absolute -top-3 -right-3 size-6 rounded-full bg-background md:hidden" />
        <span aria-hidden className="absolute -bottom-3 -left-3 hidden size-6 rounded-full bg-background md:block" />

        {!isMultiCity && (
          <Controller
            control={control}
            name="legs.0.date"
            render={({ field }) => (
              <DateField
                id="search-depart"
                label="Depart"
                value={field.value}
                onChange={field.onChange}
                minDate={today}
                error={legErrors?.[0]?.date?.message}
              />
            )}
          />
        )}
        {tripType === 'return' && (
          <Controller
            control={control}
            name="returnDate"
            render={({ field }) => (
              <DateField
                id="search-return"
                label="Return"
                value={field.value}
                onChange={field.onChange}
                minDate={legs[0]?.date ?? today}
                error={errors.returnDate?.message}
              />
            )}
          />
        )}
        <TravellersField
          id="search-travellers"
          value={{ adults, children, infants, cabin }}
          onChange={(next) => {
            const options = { shouldValidate: isSubmitted }
            setValue('adults', next.adults, options)
            setValue('children', next.children, options)
            setValue('infants', next.infants, options)
            setValue('cabin', next.cabin, options)
          }}
          error={travellerError}
        />
        <Button type="submit" size="lg"  className="mt-auto h-12 text-base font-semibold cursor-pointer">
          Search flights
        </Button>
      </div>
    </form>
  )
}
