import { createBrowserRouter } from 'react-router'
import { RootLayout } from '@/components/layout/RootLayout'
import { ForgotPasswordPage } from '@/pages/ForgotPasswordPage'
import { LandingPage } from '@/pages/LandingPage'
import { NotBuiltPage } from '@/pages/NotBuiltPage'
import { ResetPasswordPage } from '@/pages/ResetPasswordPage'
import { SearchResultsPage } from '@/pages/SearchResultsPage'
import { SignInPage } from '@/pages/SignInPage'
import { SignUpPage } from '@/pages/SignUpPage'
import { VerifyEmailPage } from '@/pages/VerifyEmailPage'

// The airline staff workspace is its own chunk: travellers never download it.
const workspace = () => import('@/pages/AirlineWorkspace')

export const router = createBrowserRouter([
  {
    Component: RootLayout,
    children: [
      { index: true, Component: LandingPage },
      { path: 'flights/search', Component: SearchResultsPage },
      { path: 'sign-in', Component: SignInPage },
      { path: 'sign-up', Component: SignUpPage },
      { path: 'verify-email', Component: VerifyEmailPage },
      { path: 'forgot-password', Component: ForgotPasswordPage },
      { path: 'reset-password', Component: ResetPasswordPage },
      { path: 'airline', lazy: async () => ({ Component: (await import('@/pages/AirlineHomePage')).AirlineHomePage }) },
      {
        path: 'airline/:airlineId',
        lazy: async () => ({ Component: (await workspace()).AirlineWorkspace }),
        children: [
          { index: true, lazy: async () => ({ Component: (await workspace()).WorkspaceIndex }) },
          { path: 'departures', lazy: async () => ({ Component: (await workspace()).DeparturesSection }) },
          { path: 'schedules', lazy: async () => ({ Component: (await workspace()).SchedulesSection }) },
          { path: 'flights', lazy: async () => ({ Component: (await workspace()).FlightsSection }) },
        ],
      },
      { path: '*', Component: NotBuiltPage },
    ],
  },
])
