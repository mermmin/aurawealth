import { Landmark } from 'lucide-react'
import { institutionOf, isLiability, useAccounts, type Account } from '../api/accounts'
import PageHeader from '../components/PageHeader'
import AccountsTable from '../components/accounts/AccountsTable'
import LinkBankButton from '../components/accounts/LinkBankButton'
import { Card, CardError, Skeleton } from '../components/Card'
import { formatMoney } from '../lib/format'

export default function AccountsPage() {
  const { data: accounts, isPending, isError, error, refetch } = useAccounts()

  return (
    <>
      <PageHeader
        page="Accounts"
        title="Every account. One clear view."
        subtitle="Your balances, organized by bank. A little less scattered, a little more settled."
        action={<LinkBankButton />}
      />

      {isPending ? (
        <div className="space-y-7">
          <div className="grid gap-6 md:grid-cols-3">
            {Array.from({ length: 3 }, (_, i) => (
              <Skeleton key={i} className="h-[124px] rounded-card" />
            ))}
          </div>
          <Skeleton className="h-96 rounded-card" />
        </div>
      ) : isError ? (
        <Card>
          <CardError message={error.message} onRetry={() => refetch()} />
        </Card>
      ) : accounts.length === 0 ? (
        <NoBanks />
      ) : (
        <div className="space-y-7">
          <Summary accounts={accounts} />
          <AccountsTable accounts={accounts} />
        </div>
      )}
    </>
  )
}

/** Same split as the backend's net worth: credit cards and loans are owed. */
function Summary({ accounts }: { accounts: Account[] }) {
  let assets = 0
  let liabilities = 0
  for (const account of accounts) {
    if (isLiability(account)) liabilities += account.currentBalance ?? 0
    else assets += account.currentBalance ?? 0
  }
  const institutions = new Set(accounts.map(institutionOf)).size

  return (
    <div className="grid gap-6 md:grid-cols-3">
      <Tile className="bg-blush" label="Total assets" value={formatMoney(assets)} caption="Checking, savings & investments" />
      <Tile className="border border-border bg-surface" label="Total liabilities" value={formatMoney(liabilities)} caption="Credit cards & loans" />
      <Tile
        className="border border-border bg-surface"
        label="Your connections"
        value={`${institutions} ${institutions === 1 ? 'institution' : 'institutions'}`}
        caption={`${accounts.length} ${accounts.length === 1 ? 'account' : 'accounts'} in total`}
      />
    </div>
  )
}

function Tile({ className, label, value, caption }: { className: string; label: string; value: string; caption: string }) {
  return (
    <section className={`rounded-card px-6 py-5 ${className}`}>
      <h2 className="text-[13px] text-ink-muted">{label}</h2>
      <p className="money mt-1 text-[32px] leading-tight font-medium text-plum">{value}</p>
      <p className="mt-1 text-[13px] text-ink-muted">{caption}</p>
    </section>
  )
}

function NoBanks() {
  return (
    <section className="flex flex-col gap-6 rounded-card border border-border bg-bg-subtle p-7 sm:flex-row sm:items-center">
      <span className="grid size-24 shrink-0 place-items-center rounded-full bg-blush text-plum">
        <Landmark className="size-9" aria-hidden />
      </span>
      <div className="flex-1">
        <h2 className="font-display text-[32px] leading-tight text-ink">Your money’s story starts here.</h2>
        <p className="mt-1 text-[15px] text-ink-muted">No banks linked yet. Link your first bank to see your accounts in one place.</p>
        <p className="mt-2 text-[13px] text-ink-muted">You’ll continue in Plaid to choose your bank and accounts.</p>
      </div>
      <LinkBankButton />
    </section>
  )
}
