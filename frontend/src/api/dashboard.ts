import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from './client'

export interface NetWorth {
  assets: number
  liabilities: number
  netWorth: number
  currency: string
  /** Oldest last-sync across the user's banks; null before the first bank is linked. */
  asOf: string | null
}

export interface CategorySpending {
  /** Plaid primary category, e.g. `FOOD_AND_DRINK`; `UNCATEGORIZED` when Plaid gave none. */
  category: string
  amount: number
}

export interface Spending {
  /** Plain dates, both inclusive. */
  from: string
  to: string
  total: number
  currency: string
  /** Biggest first. */
  categories: CategorySpending[]
}

export interface SyncResult {
  accounts: number
  added: number
  modified: number
  removed: number
}

export function useNetWorth() {
  return useQuery({ queryKey: ['net-worth'], queryFn: () => apiFetch<NetWorth>('/api/dashboard/net-worth') })
}

export function useSpending() {
  return useQuery({ queryKey: ['spending'], queryFn: () => apiFetch<Spending>('/api/dashboard/spending') })
}

export function useSync() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => apiFetch<SyncResult>('/api/plaid/sync', { method: 'POST' }),
    // A sync can change balances, spending and subscriptions; refetch everything but the user.
    onSuccess: () => queryClient.invalidateQueries({ predicate: (query) => query.queryKey[0] !== 'me' }),
  })
}
