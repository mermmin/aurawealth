import { Link } from 'react-router-dom'
import { Info, Repeat } from 'lucide-react'
import { useSubscriptions, type Subscription } from '../api/subscriptions'
import PageHeader from '../components/PageHeader'
import SubscriptionsTable from '../components/subscriptions/SubscriptionsTable'
import { Card, CardError, Skeleton } from '../components/Card'
import { formatMoney } from '../lib/format'

export default function SubscriptionsPage() {
  const { data: subscriptions, isPending, isError, error, refetch } = useSubscriptions()

  const active = subscriptions?.filter((s) => s.active) ?? []
  const inactive = subscriptions?.filter((s) => !s.active) ?? []
  const monthlyTotal = active.reduce((sum, s) => sum + s.monthlyCost, 0)

  return (
    <>
      <PageHeader
        page="Subscriptions"
        title="A little attention to what’s on repeat."
        subtitle="Your recurring payments, with the details that make them easier to keep track of."
      />

      {isPending ? (
        <div className="space-y-7">
          <Skeleton className="h-[164px] rounded-card" />
          <Skeleton className="h-96 rounded-card" />
        </div>
      ) : isError ? (
        <Card>
          <CardError message={error.message} onRetry={() => refetch()} />
        </Card>
      ) : subscriptions.length === 0 ? (
        <NothingYet />
      ) : (
        <>
          <div className="space-y-7">
            <Summary active={active} inactive={inactive.length} monthlyTotal={monthlyTotal} />
            <SubscriptionsTable active={active} inactive={inactive} monthlyTotal={monthlyTotal} />
          </div>
          <p className="mt-6 flex items-center gap-2 text-[13px] text-ink-muted">
            <Info className="size-4 shrink-0" aria-hidden />
            Billing amount is the actual recurring charge. Monthly cost is its monthly equivalent. Expected dates are
            estimates, not scheduled payments.
          </p>
        </>
      )}
    </>
  )
}

function Summary({ active, inactive, monthlyTotal }: { active: Subscription[]; inactive: number; monthlyTotal: number }) {
  return (
    <section className="grid gap-6 rounded-card bg-blush px-7 py-6 md:grid-cols-[auto_1fr] md:items-center md:gap-12 xl:grid-cols-[auto_auto_1fr]">
      <div>
        <h2 className="text-[15px] font-medium text-ink">Total active monthly cost</h2>
        <p className="mt-1 flex items-baseline gap-2 whitespace-nowrap">
          <span className="money text-[56px] leading-none font-medium tracking-tight text-plum">
            {formatMoney(monthlyTotal)}
          </span>
          <span className="text-[15px] text-ink-muted">/ month</span>
        </p>
      </div>
      <div>
        <p className="text-[17px] font-medium text-plum">
          {active.length} active {active.length === 1 ? 'subscription' : 'subscriptions'}
        </p>
        {inactive > 0 && <p className="mt-1 text-[13px] text-ink-muted">{inactive} inactive · Excluded from total</p>}
      </div>
      <div className="border-t border-border-blush pt-6 md:col-span-2 xl:col-span-1 xl:border-t-0 xl:border-l xl:pt-0 xl:pl-8">
        <h2 className="font-display text-[32px] leading-tight text-ink">Different rhythms. One monthly view.</h2>
        <p className="mt-2 text-[13px] text-ink">{rhythmNote(active)}</p>
      </div>
    </section>
  )
}

/** Explains the monthly figure with the user's own example when one exists. */
function rhythmNote(active: Subscription[]): string {
  const annual = active.find((s) => s.frequency === 'ANNUAL')
  if (annual) {
    return `Annual billing is divided by 12. For example, the ${formatMoney(annual.amount)} annual charge for ${annual.merchantName} adds ${formatMoney(annual.monthlyCost)} per month.`
  }
  const weekly = active.find((s) => s.frequency === 'WEEKLY')
  if (weekly) {
    return `Weekly billing is spread across the year. For example, the ${formatMoney(weekly.amount)} weekly charge for ${weekly.merchantName} comes to ${formatMoney(weekly.monthlyCost)} per month.`
  }
  return 'Weekly and annual charges are shown as a monthly amount, so everything adds up on one scale.'
}

function NothingYet() {
  return (
    <section className="flex flex-col gap-6 rounded-card border border-border bg-bg-subtle p-7 sm:flex-row sm:items-center">
      <span className="grid size-24 shrink-0 place-items-center rounded-full bg-blush text-plum">
        <Repeat className="size-9" aria-hidden />
      </span>
      <div>
        <h2 className="font-display text-[32px] leading-tight text-ink">Nothing on repeat yet.</h2>
        <p className="mt-1 text-[15px] text-ink-muted">
          Charges that come back every week, month or year show up here after a sync.{' '}
          <Link to="/accounts" className="font-medium text-plum hover:underline">
            Link a bank
          </Link>{' '}
          if you haven’t yet.
        </p>
      </div>
    </section>
  )
}
