import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import { ApiError, isSignedIn, subscribeAuth } from './api/client'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Client errors (401, 404…) won't fix themselves; only retry server and network failures.
      retry: (failures, error) => !(error instanceof ApiError && error.status < 500) && failures < 2,
    },
  },
})

// Drop cached data on sign-out so the next user never sees it.
subscribeAuth(() => {
  if (!isSignedIn()) queryClient.clear()
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
