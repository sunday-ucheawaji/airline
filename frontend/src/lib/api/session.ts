import type { AuthUser } from './types'

export const SessionStatus = {
  Unknown: 'unknown',
  Authenticated: 'authenticated',
  Anonymous: 'anonymous',
} as const

export type SessionStatus = (typeof SessionStatus)[keyof typeof SessionStatus]

export interface SessionSnapshot {
  status: SessionStatus
  user: AuthUser | null
}

type SessionMessage = { type: 'signed-in'; accessToken: string; user: AuthUser } | { type: 'signed-out' }

// The refresh token lives in an HttpOnly cookie the page can't read. This only remembers that one exists,
// so anonymous visitors don't call /auth/refresh on every page load.
const SIGNED_IN_HINT = 'contrail.signed-in'
const ROTATED_AT = 'contrail.auth.rotated-at'

let accessToken: string | null = null
let expiresAt = 0
let snapshot: SessionSnapshot = { status: SessionStatus.Unknown, user: null }
const listeners = new Set<() => void>()
const channel = typeof BroadcastChannel === 'undefined' ? null : new BroadcastChannel('contrail-auth')

function publish(next: SessionSnapshot) {
  snapshot = next
  listeners.forEach((listener) => listener())
}

function lifetimeMs(token: string) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    if (typeof payload.exp === 'number' && typeof payload.iat === 'number') return (payload.exp - payload.iat) * 1000
  } catch {
    // An unreadable token is treated as already expired.
  }
  return 0
}

function apply(token: string, user: AuthUser) {
  accessToken = token
  // Measured from receipt rather than from `exp`, so a wrong clock on the device doesn't matter.
  expiresAt = Date.now() + lifetimeMs(token)
  publish({ status: SessionStatus.Authenticated, user })
}

function reset() {
  accessToken = null
  expiresAt = 0
  publish({ status: SessionStatus.Anonymous, user: null })
}

channel?.addEventListener('message', (event: MessageEvent<SessionMessage>) => {
  if (event.data.type === 'signed-in') apply(event.data.accessToken, event.data.user)
  else reset()
})

export function setSession(token: string, user: AuthUser) {
  apply(token, user)
  localStorage.setItem(SIGNED_IN_HINT, '1')
  channel?.postMessage({ type: 'signed-in', accessToken: token, user } satisfies SessionMessage)
}

export function clearSession() {
  reset()
  localStorage.removeItem(SIGNED_IN_HINT)
  channel?.postMessage({ type: 'signed-out' } satisfies SessionMessage)
}

export function markAnonymous() {
  if (snapshot.status === SessionStatus.Unknown) publish({ status: SessionStatus.Anonymous, user: null })
}

export function getAccessToken() {
  return accessToken
}

export function accessTokenExpiresWithin(ms: number) {
  return !accessToken || expiresAt - Date.now() < ms
}

export function hasSessionHint() {
  return localStorage.getItem(SIGNED_IN_HINT) === '1'
}

export function markRotated() {
  localStorage.setItem(ROTATED_AT, String(Date.now()))
}

export function rotatedSince(time: number) {
  return Number(localStorage.getItem(ROTATED_AT) ?? 0) >= time
}

export function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export function getSnapshot() {
  return snapshot
}
