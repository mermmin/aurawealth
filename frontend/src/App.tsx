import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { useSignedIn } from './api/auth'
import AppShell from './components/AppShell'
import DashboardPage from './pages/DashboardPage'
import AccountsPage from './pages/AccountsPage'
import SubscriptionsPage from './pages/SubscriptionsPage'
import LoginPage from './pages/LoginPage'

export default function App() {
  return (
    <Routes>
      <Route element={<SignedOutOnly />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>
      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/accounts" element={<AccountsPage />} />
          <Route path="/subscriptions" element={<SubscriptionsPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}

function RequireAuth() {
  const signedIn = useSignedIn()
  const location = useLocation()
  // Remember where they were headed so login can send them back.
  return signedIn ? <Outlet /> : <Navigate to="/login" replace state={{ from: location.pathname }} />
}

function SignedOutOnly() {
  const signedIn = useSignedIn()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/dashboard'
  return signedIn ? <Navigate to={from} replace /> : <Outlet />
}
