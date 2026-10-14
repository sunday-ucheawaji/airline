import { api, refreshAccessToken } from './client'
import { clearSession, hasSessionHint, markAnonymous, markRotated, setSession } from './session'
import type { AuthResponse, LoginCredentials, MessageResponse } from './types'

const authRequest = { skipAuth: true, withCredentials: true }

export async function login(credentials: LoginCredentials) {
  const data = await api.post<AuthResponse, LoginCredentials>('/auth/login', credentials, authRequest)
  setSession(data.jwt, data.user)
  markRotated()
  return data.user
}

export interface SignupDetails {
  firstName: string
  lastName: string
  email: string
  password: string
}

/** Answers the same way whether or not the email already has an account. */
export function signup(details: SignupDetails) {
  return api.post<MessageResponse, SignupDetails>('/auth/signup', details, authRequest)
}

export function resendVerification(email: string) {
  return api.post<MessageResponse>('/auth/resend-verification', { email }, authRequest)
}

export function verifyEmail(token: string) {
  return api.post<MessageResponse>('/auth/verify-email', { token }, authRequest)
}

export function requestPasswordReset(email: string) {
  return api.post<MessageResponse>('/auth/forgot-password', { email }, authRequest)
}

export function resetPassword(token: string, newPassword: string) {
  return api.post<MessageResponse>('/auth/reset-password', { token, newPassword }, authRequest)
}

export async function logout() {
  try {
    await api.post('/auth/logout', undefined, authRequest)
  } finally {
    clearSession()
  }
}

export async function restoreSession() {
  if (!hasSessionHint()) {
    markAnonymous()
    return
  }
  await refreshAccessToken().catch(markAnonymous)
}
