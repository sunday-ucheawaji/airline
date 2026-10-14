import axios, { type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ApiError, toApiError } from './errors'
import {
  accessTokenExpiresWithin,
  clearSession,
  getAccessToken,
  hasSessionHint,
  markRotated,
  rotatedSince,
  setSession,
  subscribe,
} from './session'
import type { AuthResponse } from './types'

declare module 'axios' {
  interface AxiosRequestConfig {
    /** Public endpoint: never send the access token and never refresh. */
    skipAuth?: boolean
  }
  interface InternalAxiosRequestConfig {
    retriedAfterRefresh?: boolean
  }
}

const REFRESH_AHEAD_MS = 30_000
const REFRESH_LOCK = 'contrail-auth-refresh'
const HANDOFF_TIMEOUT_MS = 2_000

const baseConfig: AxiosRequestConfig = {
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 15_000,
  headers: { Accept: 'application/json' },
}

export const http = axios.create(baseConfig)

// The refresh call goes through its own instance so it can never re-enter the interceptors below.
const refreshHttp = axios.create({ ...baseConfig, withCredentials: true })

let refreshInFlight: Promise<string> | null = null

export function refreshAccessToken(): Promise<string> {
  refreshInFlight ??= refreshAcrossTabs().finally(() => {
    refreshInFlight = null
  })
  return refreshInFlight
}

// Refresh tokens rotate, and presenting one twice ends every session. Tabs share one cookie, so only one tab
// may refresh at a time; a tab that waited for the lock takes the token the winner broadcast instead.
function refreshAcrossTabs() {
  const tokenBefore = getAccessToken()
  const waitStarted = Date.now()
  const run = async () => {
    const current = getAccessToken()
    if (current && current !== tokenBefore && !accessTokenExpiresWithin(REFRESH_AHEAD_MS)) return current
    if (rotatedSince(waitStarted)) return waitForHandoff(tokenBefore)
    return callRefresh()
  }
  return navigator.locks ? navigator.locks.request(REFRESH_LOCK, run) : run()
}

async function callRefresh() {
  try {
    const { data } = await refreshHttp.post<AuthResponse>('/auth/refresh')
    setSession(data.jwt, data.user)
    markRotated()
    return data.jwt
  } catch (error) {
    const apiError = toApiError(error)
    if (apiError.kind === 'http' && apiError.status !== undefined && apiError.status < 500) clearSession()
    throw apiError
  }
}

function waitForHandoff(tokenBefore: string | null) {
  return new Promise<string>((resolve, reject) => {
    const timer = setTimeout(() => {
      unsubscribe()
      reject(new ApiError('http', 'Your session has ended. Please sign in again.', { status: 401 }))
    }, HANDOFF_TIMEOUT_MS)
    const unsubscribe = subscribe(() => {
      const token = getAccessToken()
      if (token && token !== tokenBefore) {
        clearTimeout(timer)
        unsubscribe()
        resolve(token)
      }
    })
  })
}

async function authorize(config: InternalAxiosRequestConfig) {
  if (config.skipAuth) return config

  let token = getAccessToken()
  if (accessTokenExpiresWithin(REFRESH_AHEAD_MS) && (token || hasSessionHint())) {
    token = await refreshAccessToken().catch(() => null)
  }
  if (token) config.headers.set('Authorization', `Bearer ${token}`)
  return config
}

async function recover(error: unknown) {
  const config = axios.isAxiosError(error) ? error.config : undefined
  const canRetry =
    axios.isAxiosError(error) &&
    error.response?.status === 401 &&
    config &&
    !config.skipAuth &&
    !config.retriedAfterRefresh &&
    config.headers.has('Authorization')

  if (canRetry) {
    config.retriedAfterRefresh = true
    const token = await refreshAccessToken().catch(() => null)
    if (token) {
      config.headers.set('Authorization', `Bearer ${token}`)
      return http(config)
    }
  }
  throw toApiError(error)
}

http.interceptors.request.use(authorize)
http.interceptors.response.use(undefined, recover)

export const api = {
  get: <T>(url: string, config?: AxiosRequestConfig) => http.get<T>(url, config).then((response) => response.data),
  delete: <T = void>(url: string, config?: AxiosRequestConfig) =>
    http.delete<T>(url, config).then((response) => response.data),
  post: <T = void, B = unknown>(url: string, body?: B, config?: AxiosRequestConfig) =>
    http.post<T>(url, body, config).then((response) => response.data),
  put: <T = void, B = unknown>(url: string, body?: B, config?: AxiosRequestConfig) =>
    http.put<T>(url, body, config).then((response) => response.data),
  patch: <T = void, B = unknown>(url: string, body?: B, config?: AxiosRequestConfig) =>
    http.patch<T>(url, body, config).then((response) => response.data),
}

export function newIdempotencyKey() {
  return crypto.randomUUID()
}
