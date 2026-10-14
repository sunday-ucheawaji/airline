import { z } from 'zod'

// Mirrors common-lib's RegisterRequest/LoginRequest rules and messages, so the browser catches the same mistakes the
// server would, and the server's messages read the same if a request gets past these.
export const PASSWORD_MIN = 8
const PASSWORD_MAX_BYTES = 72
const EMAIL_MAX = 254
const NAME_MAX = 100
const NAME_PATTERN = /^\p{L}[\p{L}\p{M} '.-]*$/u

const utf8Bytes = (value: string) => new TextEncoder().encode(value).length

export const emailField = z
  .string()
  .trim()
  .min(1, 'Enter your email address')
  .max(EMAIL_MAX, 'Use an email address of 254 characters or fewer')
  .pipe(z.email('Enter a valid email address, like name@example.com'))
  .transform((email) => email.toLowerCase())

export const newPasswordField = z
  .string()
  .min(1, 'Create a password')
  .min(PASSWORD_MIN, `Use at least ${PASSWORD_MIN} characters`)
  .refine((password) => utf8Bytes(password) <= PASSWORD_MAX_BYTES, {
    message: 'Use a shorter password: at most 72 bytes (some characters count as more than one)',
  })

const nameField = (required: string) =>
  z
    .string()
    .trim()
    .min(1, required)
    .max(NAME_MAX, 'Use 100 characters or fewer')
    .regex(NAME_PATTERN, 'Use letters, spaces, hyphens, apostrophes or full stops only')

export const signUpSchema = z.object({
  firstName: nameField('Enter your first name'),
  lastName: nameField('Enter your last name'),
  email: emailField,
  password: newPasswordField,
})

export const signInSchema = z.object({
  email: emailField,
  password: z.string().min(1, 'Enter your password'),
})

export const emailOnlySchema = z.object({ email: emailField })

export const resetPasswordSchema = z.object({ newPassword: newPasswordField })

export type SignUpInput = z.input<typeof signUpSchema>
export type SignUpValues = z.output<typeof signUpSchema>
export type SignInInput = z.input<typeof signInSchema>
export type SignInValues = z.output<typeof signInSchema>
export type EmailOnlyInput = z.input<typeof emailOnlySchema>
export type EmailOnlyValues = z.output<typeof emailOnlySchema>
export type ResetPasswordInput = z.input<typeof resetPasswordSchema>
export type ResetPasswordValues = z.output<typeof resetPasswordSchema>

/** Only a same-site path is followed after sign-in, never another origin or a protocol-relative URL. */
export function safeRedirect(value: string | null) {
  if (!value || !value.startsWith('/') || value.startsWith('//') || value.startsWith('/\\')) return '/'
  return value
}
