import { useSyncExternalStore } from 'react'
import { useMutation } from '@tanstack/react-query'
import {
  login,
  logout,
  requestPasswordReset,
  resendVerification,
  resetPassword,
  signup,
  verifyEmail,
  type SignupDetails,
} from './auth'
import { getSnapshot, SessionStatus, subscribe } from './session'

export function useSession() {
  const { status, user } = useSyncExternalStore(subscribe, getSnapshot)
  return { status, user, isAuthenticated: status === SessionStatus.Authenticated, isLoading: status === SessionStatus.Unknown }
}

export function useLogin() {
  return useMutation({ mutationFn: login })
}

export function useLogout() {
  return useMutation({ mutationFn: logout })
}

export function useSignup() {
  return useMutation({ mutationFn: (details: SignupDetails) => signup(details) })
}

export function useResendVerification() {
  return useMutation({ mutationFn: (email: string) => resendVerification(email) })
}

export function useVerifyEmail() {
  return useMutation({ mutationFn: (token: string) => verifyEmail(token) })
}

export function useRequestPasswordReset() {
  return useMutation({ mutationFn: (email: string) => requestPasswordReset(email) })
}

export function useResetPassword() {
  return useMutation({
    mutationFn: ({ token, newPassword }: { token: string; newPassword: string }) => resetPassword(token, newPassword),
  })
}
