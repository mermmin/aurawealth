import { Link } from 'react-router-dom'
import { useNetWorth } from '../../api/dashboard'
import { formatDate, formatMoney } from '../../lib/format'
import { CardError, Skeleton } from '../Card'

export default function NetWorthCard() {
  const { data, isPending, isError, error, refetch } = useNetWorth()

  return (
    <section className="flex flex-col rounded-card bg-blush p-7" aria-labelledby="net-worth-label">
      <div className="flex items-baseline justify-between gap-4">
        <h2 id="net-worth-label" className="text-[15px] font-medium text-ink">
          Total net worth
        </h2>
        {data?.asOf && <span className="text-[13px] text-ink-muted">As of {formatDate(data.asOf)}</span>}
      </div>

      {isPending ? (
        <>
          <Skeleton className="mt-6 h-16 w-2/3 bg-border-blush/60" />
          <Skeleton className="mt-10 h-12 w-full bg-border-blush/60" />
        </>
      ) : isError ? (
        <div className="mt-6">
          <CardError message={error.message} onRetry={() => refetch()} />
        </div>
      ) : (
        <>
          <div className="mt-5 flex items-end justify-between gap-4">
            <p className="money text-[72px] leading-none font-medium tracking-tight text-plum">
              {formatMoney(data.netWorth)}
            </p>
            <span className="pb-2 text-[13px] text-ink-muted">{data.currency}</span>
          </div>
          <p className="mt-5 text-[15px] text-ink">
            {data.asOf ? (
              'What you own, minus what you owe.'
            ) : (
              <>
                No banks linked yet.{' '}
                <Link to="/accounts" className="font-medium text-plum underline-offset-2 hover:underline">
                  Link a bank
                </Link>{' '}
                to see what you own and owe.
              </>
            )}
          </p>

          <div className="mt-6 grid grid-cols-2 gap-6 border-t border-border-blush pt-6">
            <Figure label="Total assets" amount={data.assets} caption="Checking, savings & investments" />
            <Figure label="Total liabilities" amount={data.liabilities} caption="Credit cards & loans" />
          </div>
        </>
      )}
    </section>
  )
}

function Figure({ label, amount, caption }: { label: string; amount: number; caption: string }) {
  return (
    <div>
      <p className="text-[13px] text-ink">{label}</p>
      <p className="money mt-1 text-[28px] leading-tight font-medium text-plum">{formatMoney(amount)}</p>
      <p className="mt-1 text-[13px] text-ink-muted">{caption}</p>
    </div>
  )
}
