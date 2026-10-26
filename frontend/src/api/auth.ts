import { useSyncExternalStore } from 'react'
import { apiFetch, isSignedIn, setAuth, subscribeAuth, type AuthResponse } from './client'

export interface Credentials {
  email: string
  password: string
}

export async function login(credentials: Credentials): Promise<void> {
  setAuth(await apiFetch<AuthResponse>('/api/auth/login', { method: 'POST', body: JSON.stringify(credentials) }))
}

export async function register(credentials: Credentials & { displayName?: string }): Promise<void> {
  setAuth(await apiFetch<AuthResponse>('/api/auth/register', { method: 'POST', body: JSON.stringify(credentials) }))
}

export function useSignedIn(): boolean {
  return useSyncExternalStore(subscribeAuth, isSignedIn)
}
