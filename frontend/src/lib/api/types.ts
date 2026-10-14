export interface AuthUser {
  id: string
  email: string
  firstName: string
  lastName: string
  middleName?: string | null
  phoneNumber?: string | null
  emailVerified: boolean
  status: string
  lastLogin?: string | null
}

export interface AuthResponse {
  jwt: string
  user: AuthUser
  message?: string
  title?: string
}

export interface LoginCredentials {
  email: string
  password: string
}

/** One broken rule in a validation problem: which field, which rule, and what to tell the user. */
export interface ProblemFieldError {
  field: string
  code: string
  message: string
}

/** RFC 9457 problem details, the error body of every endpoint. `detail` is always safe to show. */
export interface ProblemDetail {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  code?: string
  timestamp?: string
  errors?: ProblemFieldError[]
}

export interface MessageResponse {
  message: string
}
