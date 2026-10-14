import { isAxiosError, isCancel } from 'axios'
import type { ProblemDetail, ProblemFieldError } from './types'

export type ApiErrorKind = 'http' | 'network' | 'timeout' | 'cancelled'

const FALLBACK_MESSAGES: Record<number, string> = {
  400: 'Something in the request wasn’t right. Check the details and try again.',
  401: 'Your session has ended or you are not signed in. Please sign in and try again.',
  403: 'You don’t have access to this.',
  404: 'We couldn’t find what you were looking for.',
  409: 'This changed while you were working on it. Refresh and try again.',
  429: 'Too many requests. Please wait a moment and try again.',
  500: 'Something went wrong on our side. Please try again later.',
  502: 'This service is temporarily unavailable. Please try again in a moment.',
  503: 'This service is temporarily unavailable. Please try again in a moment.',
  504: 'This is taking longer than it should. Please try again.',
}

const KIND_MESSAGES: Record<Exclude<ApiErrorKind, 'http'>, string> = {
  network: 'We couldn’t reach Contrail. Check your connection and try again.',
  timeout: 'This is taking longer than it should. Try again.',
  cancelled: 'The request was cancelled.',
}

export class ApiError extends Error {
  readonly kind: ApiErrorKind
  readonly status?: number
  /** The server's stable error code, e.g. INVALID_CREDENTIALS or VALIDATION_FAILED. */
  readonly code?: string
  /** Per-field validation messages, in the order the server reported them. */
  readonly fieldErrors: ProblemFieldError[]
  readonly path?: string
  readonly requestId?: string

  constructor(
    kind: ApiErrorKind,
    message: string,
    details: { status?: number; code?: string; fieldErrors?: ProblemFieldError[]; path?: string; requestId?: string; cause?: unknown } = {},
  ) {
    super(message, { cause: details.cause })
    this.name = 'ApiError'
    this.kind = kind
    this.status = details.status
    this.code = details.code
    this.fieldErrors = details.fieldErrors ?? []
    this.path = details.path
    this.requestId = details.requestId
  }

  get isUnauthorized() {
    return this.status === 401
  }

  get isForbidden() {
    return this.status === 403
  }

  get isNotFound() {
    return this.status === 404
  }
}

export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error

  if (isCancel(error)) return new ApiError('cancelled', KIND_MESSAGES.cancelled, { cause: error })

  if (isAxiosError<ProblemDetail>(error)) {
    if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
      return new ApiError('timeout', KIND_MESSAGES.timeout, { cause: error })
    }
    if (!error.response) return new ApiError('network', KIND_MESSAGES.network, { cause: error })

    const { status, data, headers } = error.response
    const body = typeof data === 'object' && data !== null ? data : undefined
    const requestId = headers['x-request-id']
    return new ApiError('http', body?.detail || FALLBACK_MESSAGES[status] || 'Something went wrong. Try again.', {
      status,
      code: body?.code,
      fieldErrors: Array.isArray(body?.errors) ? body.errors : undefined,
      path: body?.instance,
      requestId: typeof requestId === 'string' ? requestId : undefined,
      cause: error,
    })
  }

  return new ApiError('network', error instanceof Error ? error.message : 'Something went wrong. Try again.', { cause: error })
}

export function isRetryable(error: unknown) {
  const apiError = toApiError(error)
  if (apiError.kind === 'network' || apiError.kind === 'timeout') return true
  return apiError.status === 429 || apiError.status === 502 || apiError.status === 503 || apiError.status === 504
}
