import { useState, type FormEvent, type ReactNode } from 'react'
import { Flower, Loader2 } from 'lucide-react'
import { login, register } from '../api/auth'

type Mode = 'login' | 'register'

export default function LoginPage() {
  const [mode, setMode] = useState<Mode>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const registering = mode === 'register'

  function switchMode() {
    setMode(registering ? 'login' : 'register')
    setError(null)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    // The backend's 400 doesn't name the field, so check the password length here.
    if (registering && password.length < 8) {
      setError('Password needs at least 8 characters.')
      return
    }
    setSubmitting(true)
    try {
      if (registering) {
        await register({ email, password, displayName: displayName.trim() || undefined })
      } else {
        await login({ email, password })
      }
      // Signing in flips the route guard, which redirects away from this page.
    } catch (err) {
      setError(err instanceof Error ? friendly(err.message) : 'Something went wrong. Please try again.')
      setSubmitting(false)
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-bg px-4 py-12">
      <div className="w-full max-w-md">
        <div className="flex items-center justify-center gap-2 text-plum">
          <Flower className="size-7" strokeWidth={1.5} aria-hidden />
          <span className="font-display text-4xl leading-none">aurawealth</span>
          <span className="size-1.5 rounded-full bg-pink" aria-hidden />
        </div>

        <div className="mt-8 rounded-card border border-border bg-surface p-8">
          <h1 className="font-display text-4xl leading-tight text-ink">
            {registering ? 'A little clarity starts here.' : 'Welcome back.'}
          </h1>
          <p className="mt-1 text-[15px] text-ink-muted">
            {registering
              ? 'Create an account to see your money in one calm place.'
              : 'Sign in to see where your money stands today.'}
          </p>

          <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
            {registering && (
              <Field label="Your name" hint="Optional. We’ll use it to say hello.">
                <input
                  type="text"
                  autoComplete="name"
                  maxLength={100}
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  className={inputClass}
                />
              </Field>
            )}
            <Field label="Email">
              <input
                type="email"
                autoComplete="email"
                required
                maxLength={255}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className={inputClass}
              />
            </Field>
            <Field label="Password" hint={registering ? 'At least 8 characters.' : undefined}>
              <input
                type="password"
                autoComplete={registering ? 'new-password' : 'current-password'}
                required
                minLength={registering ? 8 : undefined}
                maxLength={72}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className={inputClass}
              />
            </Field>

            {error && (
              <p role="alert" className="rounded-button bg-blush px-3 py-2 text-[13px] text-plum">
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={submitting}
              className="flex h-10 w-full items-center justify-center gap-2 rounded-button bg-plum text-[15px] font-medium text-white hover:bg-plum/90 disabled:opacity-70"
            >
              {submitting && <Loader2 className="size-4 animate-spin" aria-hidden />}
              {registering ? 'Create account' : 'Sign in'}
            </button>
          </form>
        </div>

        <p className="mt-6 text-center text-[13px] text-ink-muted">
          {registering ? 'Already have an account?' : 'New here?'}{' '}
          <button type="button" onClick={switchMode} className="font-medium text-plum hover:underline">
            {registering ? 'Sign in' : 'Create an account'}
          </button>
        </p>
      </div>
    </main>
  )
}

const inputClass =
  'mt-1 block h-10 w-full rounded-button border border-border bg-surface px-3 text-[15px] text-ink outline-none focus:border-plum focus:ring-2 focus:ring-blush'

function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="text-[13px] font-medium text-ink">{label}</span>
      {children}
      {hint && <span className="mt-1 block text-[13px] text-ink-muted">{hint}</span>}
    </label>
  )
}

// Backend details are written for developers; reword the ones people will see.
function friendly(detail: string): string {
  switch (detail) {
    case 'Email is already registered':
      return 'That email already has an account. Try signing in instead.'
    case 'Invalid email or password':
      return 'That email and password don’t match. Please try again.'
    case 'Invalid request content.':
      return 'Please check your email address and password.'
    default:
      return detail
  }
}
