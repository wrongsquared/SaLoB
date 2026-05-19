import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

async function enableMocking() {
  const { worker } = await import('./shared/test/mocks/browser')
  return worker.start({ onUnhandledRequest: 'bypass' })
}

if (import.meta.env.DEV) {
  enableMocking()
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
