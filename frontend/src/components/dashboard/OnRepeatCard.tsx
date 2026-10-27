import { Link } from 'react-router-dom'
import { ArrowUpRight } from 'lucide-react'
import { useSubscriptions } from '../../api/subscriptions'
import { categoryIcon } from '../../lib/categories'
import { formatDate, formatMoney } from '../../lib/format'
import { Card, CardError, CardTitle, Skeleton } from '../Card'

const SHOWN = 3

export default function OnRepeatCard() {
  const { data: subscriptions, isPending, isError, error, refetch } = useSubscriptions()

  const active = subscriptions?.filter((s) => s.active) ?? []
  const monthly = active.reduce((sum, s) => sum + s.monthlyCost, 0)
  const annual = active.filter((s) => s.frequency === 'ANNUAL')
  const annualMonthly = annual.reduce((sum, s) => sum + s.monthlyCost, 0)
  // The API already orders active subscriptions by next charge.
  const next = active.slice(0, SHOWN)

  return (
    <Card>
      <CardTitle
        action={
          (subscriptions?.length ?? 0) > 0 && (
            <Link
              to="/subscriptions"
              className="flex items-center gap-1 text-[15px] font-medium text-plum hover:underline"
            >
              View ledger
              <ArrowUpRight className="size-4" aria-hidden />
            </Link>
          )
        }
      >
        On repeat
      </CardTitle>

      {isPending ? (
        <>
          <Skeleton className="mt-6 h-11 w-40" />
          <div className="mt-8 space-y-4">
            {Array.from({ length: SHOWN }, (_, i) => (
              <Skeleton key={i} className="h-12" />
            ))}
          </div>
        </>
      ) : isError ? (
        <div className="mt-6">
          <CardError message={error.message} onRetry={() => refetch()} />
        </div>
      ) : active.length === 0 ? (
        <p className="mt-3 text-[15px] text-ink-muted">
          Nothing on repeat right now. Charges that come back every week, month or year show up here after a sync.
        </p>
      ) : (
        <>
          <p className="mt-5 flex items-baseline gap-2">
            <span className="money text-[44px] leading-none font-medium tracking-tight text-ink">
              {formatMoney(monthly)}
            </span>
            <span className="text-[15px] text-ink-muted">/ month</span>
          </p>
          <p className="mt-4 text-[13px] text-ink-muted">
            {active.length} active {active.length === 1 ? 'subscription' : 'subscriptions'}
          </p>

          <h3 className="mt-6 text-[12px] font-medium tracking-wider text-ink-muted uppercase">
            Next expected charges
          </h3>
          <ul className="mt-2">
            {next.map((s) => {
              const Icon = categoryIcon(s.category)
              return (
                <li key={s.id} className="flex items-center gap-3 border-b border-border py-3">
                  <span className="grid size-10 shrink-0 place-items-center rounded-tile border border-border bg-bg-subtle text-plum">
                    <Icon className="size-[18px]" aria-hidden />
                  </span>
                  <span className="min-w-0 flex-1 truncate text-[15px] text-ink">{s.merchantName}</span>
                  <span className="text-right">
                    <span className="money block text-[15px] font-medium text-ink">{formatMoney(s.amount)}</span>
                    <span className="block text-[13px] text-ink-muted">{formatDate(s.nextExpectedOn)}</span>
                  </span>
                </li>
              )
            })}
          </ul>

          {annual.length > 0 && (
            <p className="mt-6 text-[13px] text-ink-muted">
              Monthly cost includes {formatMoney(annualMonthly)} for {annual.length === 1 ? 'an annual plan' : 'annual plans'}.
              Your actual charge follows its billing frequency.
            </p>
          )}
        </>
      )}
    </Card>
  )
}
