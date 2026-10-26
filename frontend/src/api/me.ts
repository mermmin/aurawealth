import { useQuery } from '@tanstack/react-query'
import { apiFetch } from './client'

export interface Me {
  id: number
  email: string
  displayName: string | null
}

export function useMe() {
  return useQuery({ queryKey: ['me'], queryFn: () => apiFetch<Me>('/api/me') })
}

/** displayName when set, otherwise the email's local part. */
export function nameOf(me: Me): string {
  return me.displayName ?? me.email.split('@')[0]
}
