import { Link } from 'react-router-dom'
import { ArrowUpRight } from 'lucide-react'
import { institutionOf, signedBalance, useAccounts } from '../../api/accounts'
import { formatMoney } from '../../lib/format'
import BankTile from '../BankTile'
import { Card, CardError, CardTitle, Skeleton } from '../Card'

const SHOWN = 4

export default function AccountsCard() {
  const { data: accounts, isPending, isError, error, refetch } = useAccounts()

  const total = accounts?.length ?? 0
  const institutions = new Set(accounts?.map(institutionOf)).size
  // The biggest balances either way tell the story; the full list lives on the Accounts page.
  const largest = [...(accounts ?? [])]
    .sort((a, b) => Math.abs(signedBalance(b)) - Math.abs(signedBalance(a)))
    .slice(0, SHOWN)

  return (
    <Card>
      <CardTitle
        action={
          total > 0 && (
            <Link to="/accounts" className="flex items-center gap-1 text-[15px] font-medium text-plum hover:underline">
              {total > SHOWN ? `View all ${total}` : 'View accounts'}
              <ArrowUpRight className="size-4" aria-hidden />
            </Link>
          )
        }
      >
        Your accounts
      </CardTitle>

      {isPending ? (
        <div className="mt-6 space-y-4">
          {Array.from({ length: SHOWN }, (_, i) => (
            <Skeleton key={i} className="h-10" />
          ))}
        </div>
      ) : isError ? (
        <div className="mt-6">
          <CardError message={error.message} onRetry={() => refetch()} />
        </div>
      ) : total === 0 ? (
        <p className="mt-3 text-[15px] text-ink-muted">
          Nothing here yet.{' '}
          <Link to="/accounts" className="font-medium text-plum hover:underline">
            Link your first bank
          </Link>{' '}
          to see your balances in one place.
        </p>
      ) : (
        <>
          <p className="mt-1 text-[13px] text-ink-muted">
            {plural(total, 'account')} across {plural(institutions, 'institution')}
          </p>
          <ul className="mt-4">
            {largest.map((account) => (
              <li key={account.id} className="flex items-center gap-3 border-b border-border py-3">
                <BankTile institution={institutionOf(account)} />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-[15px] text-ink">{account.name}</p>
                  <p className="truncate text-[13px] text-ink-muted">
                    {institutionOf(account)}
                    {account.mask && ` · ••••${account.mask}`}
                  </p>
                </div>
                <span className="money text-[15px] font-medium text-ink">{formatMoney(signedBalance(account))}</span>
              </li>
            ))}
          </ul>
        </>
      )}
    </Card>
  )
}

function plural(n: number, word: string): string {
  return `${n} ${word}${n === 1 ? '' : 's'}`
}
