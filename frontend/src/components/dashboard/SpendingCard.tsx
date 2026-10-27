import { Pie, PieChart } from 'recharts'
import { useSpending, type CategorySpending } from '../../api/dashboard'
import { categoryLabel } from '../../lib/categories'
import { formatDateRange, formatMoney } from '../../lib/format'
import { Card, CardError, CardTitle, Skeleton } from '../Card'

/** §9 chart colors, in size order. */
const COLORS = ['#512c48', '#c890a5', '#ada0bc', '#dfd8de', '#dfccbb']
// Five slices fit the palette; past that, the top four keep their names and the rest share one.
const NAMED = COLORS.length - 1
const DONUT = 200

interface Slice {
  label: string
  amount: number
  fill: string
}

export default function SpendingCard() {
  const { data, isPending, isError, error, refetch } = useSpending()

  return (
    <Card>
      <CardTitle>Where it went</CardTitle>

      {isPending ? (
        <>
          <Skeleton className="mt-6 h-11 w-48" />
          <div className="mt-8 flex items-center gap-10">
            <Skeleton className="size-[200px] shrink-0 rounded-full" />
            <Skeleton className="h-40 flex-1" />
          </div>
        </>
      ) : isError ? (
        <div className="mt-6">
          <CardError message={error.message} onRetry={() => refetch()} />
        </div>
      ) : (
        <>
          <p className="money mt-5 text-[44px] leading-none font-medium tracking-tight text-ink">
            {formatMoney(data.total)}
          </p>
          <div className="mt-3 flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1 text-[13px] text-ink-muted">
            <span>Spent in the last 30 days</span>
            <span>{formatDateRange(data.from, data.to)}</span>
          </div>

          {data.categories.length === 0 ? (
            <p className="mt-8 rounded-card bg-bg-subtle px-6 py-10 text-center text-[15px] text-ink-muted">
              No spending in the last 30 days.
            </p>
          ) : (
            <Breakdown categories={data.categories} />
          )}
        </>
      )}
    </Card>
  )
}

function Breakdown({ categories }: { categories: CategorySpending[] }) {
  const slices = toSlices(categories)
  const count = categories.length

  return (
    <>
      <div className="mt-8 flex flex-col items-center gap-8 sm:flex-row sm:gap-12">
        <div className="relative shrink-0" style={{ width: DONUT, height: DONUT }}>
          {/* The legend carries every number, so the chart itself is decoration for screen readers. */}
          <div aria-hidden>
            <PieChart width={DONUT} height={DONUT} accessibilityLayer={false}>
              <Pie
                data={slices}
                dataKey="amount"
                nameKey="label"
                innerRadius={DONUT / 2 - 22}
                outerRadius={DONUT / 2}
                startAngle={90}
                endAngle={-270}
                stroke="#ffffff"
                strokeWidth={3}
                isAnimationActive={false}
              />
            </PieChart>
          </div>
          <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
            <span className="text-[13px] text-ink-muted">Last 30 days</span>
            <span className="font-display text-[30px] leading-tight text-ink">
              {count} {count === 1 ? 'category' : 'categories'}
            </span>
          </div>
        </div>

        <ul className="w-full flex-1 space-y-3">
          {slices.map((slice) => (
            <li key={slice.label} className="flex items-center gap-3 text-[15px]">
              <span className="size-2 shrink-0 rounded-full" style={{ backgroundColor: slice.fill }} aria-hidden />
              <span className="flex-1 text-ink-muted">{slice.label}</span>
              <span className="money font-medium text-ink">{formatMoney(slice.amount)}</span>
            </li>
          ))}
        </ul>
      </div>
      <p className="mt-8 text-[13px] text-ink-muted">Categories shown from largest to smallest.</p>
    </>
  )
}

function toSlices(categories: CategorySpending[]): Slice[] {
  const named = categories.length <= COLORS.length ? categories : categories.slice(0, NAMED)
  const slices = named.map((c, i) => ({ label: categoryLabel(c.category), amount: c.amount, fill: COLORS[i] }))
  const rest = categories.slice(named.length)
  if (rest.length > 0) {
    const amount = rest.reduce((sum, c) => sum + c.amount, 0)
    slices.push({ label: 'Everything else', amount, fill: COLORS[NAMED] })
  }
  return slices
}
