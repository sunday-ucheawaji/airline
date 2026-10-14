import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router/dom'
import './index.css'
import { ApiProvider, restoreSession } from './lib/api'
import { router } from './router'

void restoreSession()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ApiProvider>
      <RouterProvider router={router} />
    </ApiProvider>
  </StrictMode>,
)
