export { ApiProvider } from './ApiProvider'
export { login, logout, restoreSession, type SignupDetails } from './auth'
export { api, http, newIdempotencyKey } from './client'
export { ApiError, isRetryable, toApiError, type ApiErrorKind } from './errors'
export {
  useLogin,
  useLogout,
  useRequestPasswordReset,
  useResendVerification,
  useResetPassword,
  useSession,
  useSignup,
  useVerifyEmail,
} from './hooks'
export { queryClient } from './queryClient'
export { SessionStatus, type SessionSnapshot } from './session'
export type { AuthResponse, AuthUser, LoginCredentials, MessageResponse, ProblemDetail, ProblemFieldError } from './types'
