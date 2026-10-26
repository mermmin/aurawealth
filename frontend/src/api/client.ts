// Holds the JWT and wraps fetch so every /api call carries it.

const STORAGE_KEY = 'aurawealth.auth'

interface StoredAuth {
  accessToken: string
  expiresAt: string
}

export interface AuthResponse extends StoredAuth {
  tokenType: string
}

/** A failed API call, with the problem-JSON `detail` when the backend sent one. */
export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

const listeners = new Set<() => void>()
let expiryTimer: ReturnType<typeof setTimeout> | undefined

function readAuth(): StoredAuth | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const auth = JSON.parse(raw) as StoredAuth
    return Date.parse(auth.expiresAt) > Date.now() ? auth : null
  } catch {
    return null
  }
}

let current = readAuth()

function notify() {
  listeners.forEach((listener) => listener())
}

function scheduleExpiry() {
  clearTimeout(expiryTimer)
  if (current) {
    expiryTimer = setTimeout(clearAuth, Date.parse(current.expiresAt) - Date.now())
  }
}

export function setAuth(auth: AuthResponse) {
  current = { accessToken: auth.accessToken, expiresAt: auth.expiresAt }
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(current))
  } catch {
    // Storage blocked (private mode): stay signed in for this tab only.
  }
  scheduleExpiry()
  notify()
}

export function clearAuth() {
  if (!current) return
  current = null
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    // Nothing stored to remove.
  }
  scheduleExpiry()
  notify()
}

export function isSignedIn(): boolean {
  return current !== null
}

export function subscribeAuth(listener: () => void): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

scheduleExpiry()

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body) headers.set('Content-Type', 'application/json')
  if (current) headers.set('Authorization', `Bearer ${current.accessToken}`)

  const res = await fetch(path, { ...init, headers })

  if (!res.ok) {
    // A rejected token means the session is over; a 401 from login itself is just bad credentials.
    if (res.status === 401 && !path.startsWith('/api/auth/')) clearAuth()
    throw new ApiError(res.status, await problemDetail(res))
  }
  return res.status === 204 ? (undefined as T) : res.json()
}

async function problemDetail(res: Response): Promise<string> {
  try {
    const body = await res.json()
    if (typeof body.detail === 'string') return body.detail
  } catch {
    // Not JSON (e.g. the proxy's own error page when the backend is down).
  }
  return res.status >= 500 ? 'Something went wrong on our side. Please try again.' : res.statusText
}
