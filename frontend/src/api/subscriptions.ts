import { useQuery } from '@tanstack/react-query'
import { apiFetch } from './client'

export type Frequency = 'WEEKLY' | 'MONTHLY' | 'ANNUAL'

export interface Subscription {
  id: number
  merchantName: string
  amount: number
  currency: string | null
  frequency: Frequency
  /** Weekly and annual charges scaled to a month, so every row can be added up. */
  monthlyCost: number
  lastChargedOn: string
  /** Plain date; for inactive rows it's when the charge would have come. */
  nextExpectedOn: string
  category: string | null
  active: boolean
}

/** Sorted by the API: active first, then by next expected charge. */
export function useSubscriptions() {
  return useQuery({ queryKey: ['subscriptions'], queryFn: () => apiFetch<Subscription[]>('/api/subscriptions') })
}
