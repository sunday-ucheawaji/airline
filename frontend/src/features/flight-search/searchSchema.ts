import { startOfToday } from 'date-fns'
import { z } from 'zod'

export const TRIP_TYPES = ['return', 'one-way', 'multi-city'] as const
export type TripType = (typeof TRIP_TYPES)[number]

export const TRIP_TYPE_LABELS: Record<TripType, string> = {
  return: 'Return',
  'one-way': 'One way',
  'multi-city': 'Multi-city',
}

export const CABINS = ['ECONOMY', 'PREMIUM_ECONOMY', 'BUSINESS', 'FIRST'] as const
export type Cabin = (typeof CABINS)[number]

export const CABIN_LABELS: Record<Cabin, string> = {
  ECONOMY: 'Economy',
  PREMIUM_ECONOMY: 'Premium economy',
  BUSINESS: 'Business',
  FIRST: 'First',
}

export const MAX_SEATED_TRAVELLERS = 9
export const MAX_LEGS = 5

// Required-field checks live in superRefine so every problem is reported in one pass.
const legSchema = z.object({
  from: z.string(),
  to: z.string(),
  date: z.date().optional(),
})

export const searchSchema = z
  .object({
    tripType: z.enum(TRIP_TYPES),
    legs: z.array(legSchema).min(1).max(MAX_LEGS),
    returnDate: z.date().optional(),
    adults: z.number().int().min(1).max(MAX_SEATED_TRAVELLERS),
    children: z.number().int().min(0).max(MAX_SEATED_TRAVELLERS - 1),
    infants: z.number().int().min(0),
    cabin: z.enum(CABINS),
  })
  .superRefine((search, ctx) => {
    const today = startOfToday()
    const issue = (path: (string | number)[], message: string) =>
      ctx.addIssue({ code: 'custom', message, path })

    search.legs.forEach((leg, index) => {
      if (!leg.from) issue(['legs', index, 'from'], 'Choose where you’re flying from')
      if (!leg.to) issue(['legs', index, 'to'], 'Choose where you’re flying to')
      else if (leg.from === leg.to) issue(['legs', index, 'to'], 'Choose a destination different from where you start')

      const previous = search.legs[index - 1]
      if (!leg.date) issue(['legs', index, 'date'], 'Choose a departure date')
      else if (leg.date < today) issue(['legs', index, 'date'], 'Choose today or a later date')
      else if (previous?.date && leg.date < previous.date)
        issue(['legs', index, 'date'], 'This flight can’t leave before the one above it')
    })

    if (search.tripType === 'return') {
      const departure = search.legs[0]?.date
      if (!search.returnDate) issue(['returnDate'], 'Choose a return date')
      else if (departure && search.returnDate < departure) issue(['returnDate'], 'Return on or after your departure date')
    }

    if (search.adults + search.children > MAX_SEATED_TRAVELLERS) {
      issue(['children'], `Book up to ${MAX_SEATED_TRAVELLERS} seated travellers at a time`)
    }
    if (search.infants > search.adults) {
      issue(['infants'], 'Each infant travels on an adult’s lap, so add an adult for every infant')
    }
  })

export type FlightSearch = z.infer<typeof searchSchema>
