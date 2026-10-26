import { useQuery } from '@tanstack/react-query'
import { apiFetch } from './client'

export interface Account {
  id: number
  institutionName: string | null
  name: string
  mask: string | null
  type: string
  subtype: string | null
  currentBalance: number | null
  availableBalance: number | null
  isoCurrencyCode: string | null
  updatedAt: string
}

export function useAccounts() {
  return useQuery({ queryKey: ['accounts'], queryFn: () => apiFetch<Account[]>('/api/accounts') })
}

/** Matches the backend's net-worth split: credit cards and loans are owed, everything else is owned. */
export function isLiability(account: Account): boolean {
  return account.type === 'credit' || account.type === 'loan'
}

/** Balance as it counts toward net worth: what you owe is negative. */
export function signedBalance(account: Account): number {
  const balance = account.currentBalance ?? 0
  return isLiability(account) ? -balance : balance
}

export function institutionOf(account: Account): string {
  return account.institutionName ?? 'Other bank'
}
