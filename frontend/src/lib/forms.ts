import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { toApiError, type ApiError } from '@/lib/api'

/**
 * Shows a validation problem's messages under the matching fields, first message per field, and focuses the first
 * one. `aliases` maps an API field name to the form field that edits it (e.g. `departureAirportId` → `departure`).
 * Returns the error when something is left for a form-level alert: no field matched, or it wasn't a field error.
 */
export function applyServerErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  fields: readonly Path<T>[],
  aliases: Partial<Record<string, Path<T>>> = {},
): ApiError | null {
  const apiError = toApiError(error)
  const known = new Set<string>(fields)
  const placed = new Set<string>()

  for (const fieldError of apiError.fieldErrors) {
    const field = aliases[fieldError.field] ?? fieldError.field
    if (!known.has(field) || placed.has(field)) continue
    setError(field as Path<T>, { type: 'server', message: fieldError.message }, { shouldFocus: placed.size === 0 })
    placed.add(field)
  }
  return placed.size > 0 ? null : apiError
}
