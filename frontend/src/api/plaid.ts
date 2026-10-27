import { useCallback, useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { usePlaidLink, type PlaidLinkOnExit, type PlaidLinkOnSuccess } from 'react-plaid-link'
import { apiFetch } from './client'
import type { SyncResult } from './dashboard'

interface LinkToken {
  linkToken: string
  expiration: string
}

/**
 * Link a bank: get a link token from the backend, open Plaid Link with it, exchange the
 * public token it returns, then sync once so transactions and subscriptions arrive too.
 */
export function useLinkBank() {
  const queryClient = useQueryClient()
  const [linkToken, setLinkToken] = useState<string | null>(null)
  const [exitError, setExitError] = useState<string | null>(null)

  const start = useMutation({
    mutationFn: () => apiFetch<LinkToken>('/api/plaid/link-token', { method: 'POST' }),
    onSuccess: (data) => setLinkToken(data.linkToken),
  })

  const finish = useMutation({
    mutationFn: async (publicToken: string) => {
      try {
        await apiFetch('/api/plaid/exchange', { method: 'POST', body: JSON.stringify({ publicToken }) })
      } catch (e) {
        throw new Error(`Couldn’t link your bank. ${(e as Error).message}.`)
      }
      try {
        await apiFetch<SyncResult>('/api/plaid/sync', { method: 'POST' })
      } catch {
        throw new Error('Your bank is linked, but its transactions didn’t load yet. Press Sync on the dashboard to try again.')
      }
    },
    // Even a half-finished link adds accounts; refetch everything but the user.
    onSettled: () => queryClient.invalidateQueries({ predicate: (query) => query.queryKey[0] !== 'me' }),
  })

  const onSuccess = useCallback<PlaidLinkOnSuccess>(
    (publicToken) => {
      setLinkToken(null)
      if (publicToken) finish.mutate(publicToken)
    },
    [finish],
  )

  // Closing Link on purpose isn't an error; only report what Plaid flags.
  const onExit = useCallback<PlaidLinkOnExit>((error) => {
    setLinkToken(null)
    if (error) setExitError(`Couldn’t link your bank. ${error.display_message ?? 'Plaid stopped before it finished.'}`)
  }, [])

  const { open, ready } = usePlaidLink({ token: linkToken, onSuccess, onExit })

  useEffect(() => {
    if (linkToken && ready) open()
  }, [linkToken, ready, open])

  const { mutate: startLink, reset: resetStart } = start
  const { reset: resetFinish } = finish
  const begin = useCallback(() => {
    resetStart()
    resetFinish()
    setExitError(null)
    startLink()
  }, [resetStart, resetFinish, startLink])

  return {
    begin,
    phase: finish.isPending ? 'linking' : start.isPending || linkToken ? 'opening' : 'idle',
    /** A full sentence, ready to show. */
    error: (start.error && `Couldn’t open Plaid. ${start.error.message}.`) || finish.error?.message || exitError,
  } as const
}

export type LinkBank = ReturnType<typeof useLinkBank>
