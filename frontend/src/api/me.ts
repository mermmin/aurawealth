import { useQuery } from '@tanstack/react-query'

export interface Me {
  id: number
  email: string
  displayName: string | null
}

// Bearer token is added in Session 20 (auth); until then this returns 401.
async function fetchMe(): Promise<Me> {
  const res = await fetch('/api/me')
  if (!res.ok) throw new Error(`GET /api/me failed: ${res.status}`)
  return res.json()
}

export function useMe() {
  return useQuery({ queryKey: ['me'], queryFn: fetchMe, retry: false })
}

/** displayName when set, otherwise the email's local part. */
export function nameOf(me: Me): string {
  return me.displayName ?? me.email.split('@')[0]
}
