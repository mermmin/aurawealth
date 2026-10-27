import type { Frequency, Subscription } from '../../api/subscriptions'
import { categoryIcon, categoryLabel } from '../../lib/categories'
import { formatDate, formatMoney } from '../../lib/format'
import { Card, CardTitle } from '../Card'

const FREQUENCY_LABELS: Record<Frequency, string> = { WEEKLY: 'Weekly', MONTHLY: 'Monthly', ANNUAL: 'Annual' }

const COLUMNS = 8

interface Props {
  active: Subscription[]
  inactive: Subscription[]
  monthlyTotal: number
}

export default function SubscriptionsTable({ active, inactive, monthlyTotal }: Props) {
  const count = active.length + inactive.length

  return (
    <Card>
      <CardTitle
        action={
          <span className="text-[13px] text-ink-muted">
            {count} {count === 1 ? 'subscription' : 'subscriptions'} · USD
          </span>
        }
      >
        Your subscriptions
      </CardTitle>

      <div className="mt-6 overflow-x-auto">
        <table className="w-full min-w-[960px] text-left text-[15px] whitespace-nowrap">
          <thead>
            <tr className="border-y border-border bg-bg-subtle text-[13px] text-ink-muted">
              <th scope="col" className="px-5 py-3 font-normal">Merchant</th>
              <th scope="col" className="px-3 py-3 font-normal">Category</th>
              <th scope="col" className="px-3 py-3 text-right font-normal">Billing amount</th>
              <th scope="col" className="px-3 py-3 font-normal">Frequency</th>
              <th scope="col" className="px-3 py-3 text-right font-medium text-plum">Monthly cost</th>
              <th scope="col" className="px-3 py-3 font-normal">Last charged</th>
              <th scope="col" className="px-3 py-3 font-normal">Next expected</th>
              <th scope="col" className="px-5 py-3 text-center font-normal">Status</th>
            </tr>
          </thead>

          {active.length > 0 && (
            <tbody>
              <GroupRow label={`Active · ${active.length}`} note="By next expected charge" />
              {active.map((s) => (
                <SubscriptionRow key={s.id} subscription={s} />
              ))}
            </tbody>
          )}

          {inactive.length > 0 && (
            <tbody>
              <GroupRow
                label={`Inactive · ${inactive.length}`}
                note="Not included in monthly total · No charge expected"
                muted
              />
              {inactive.map((s) => (
                <SubscriptionRow key={s.id} subscription={s} />
              ))}
            </tbody>
          )}

          <tfoot>
            <tr>
              <th scope="row" colSpan={COLUMNS - 3} className="px-5 pt-6 text-left text-[15px] font-medium text-ink">
                Total active monthly cost
              </th>
              <td colSpan={3} className="money px-5 pt-6 text-right text-[28px] font-medium text-plum">
                {formatMoney(monthlyTotal)} / month
              </td>
            </tr>
          </tfoot>
        </table>
      </div>
    </Card>
  )
}

function GroupRow({ label, note, muted }: { label: string; note: string; muted?: boolean }) {
  return (
    <tr className={`border-b border-border ${muted ? 'bg-bg-subtle' : ''}`}>
      <th scope="colgroup" colSpan={COLUMNS} className="px-5 py-3 text-left font-normal">
        <span className="flex items-baseline justify-between gap-4">
          <span className={`text-[12px] font-medium tracking-wider uppercase ${muted ? 'text-ink-muted' : 'text-plum'}`}>
            {label}
          </span>
          <span className="text-[13px] text-ink-muted">{note}</span>
        </span>
      </th>
    </tr>
  )
}

function SubscriptionRow({ subscription: s }: { subscription: Subscription }) {
  const Icon = categoryIcon(s.category)
  const text = s.active ? 'text-ink' : 'text-ink-inactive'

  return (
    <tr className="border-b border-border">
      <td className="px-5 py-4">
        <span className="flex items-center gap-3">
          <span className="grid size-10 shrink-0 place-items-center rounded-tile border border-border bg-bg-subtle text-plum">
            <Icon className="size-[18px]" aria-hidden />
          </span>
          <span className={`font-medium ${text}`}>{s.merchantName}</span>
        </span>
      </td>
      <td className="px-3 py-4 text-ink-muted">{categoryLabel(s.category)}</td>
      <td className={`money px-3 py-4 text-right ${text}`}>{formatMoney(s.amount)}</td>
      <td className="px-3 py-4 text-ink-muted">{FREQUENCY_LABELS[s.frequency]}</td>
      <td className={`money px-3 py-4 text-right font-medium ${s.active ? 'text-plum' : 'text-ink-inactive'}`}>
        {formatMoney(s.monthlyCost)}
      </td>
      <td className={`px-3 py-4 ${text}`}>{formatDate(s.lastChargedOn)}</td>
      <td className={`px-3 py-4 ${text}`}>
        {s.active ? formatDate(s.nextExpectedOn) : <span aria-label="No charge expected">—</span>}
      </td>
      <td className="px-5 py-4 text-center">
        {s.active ? (
          <span className="rounded-full bg-success-bg px-3 py-1 text-[12px] font-medium text-success">Active</span>
        ) : (
          <span className="rounded-full border border-border bg-inactive-badge-bg px-3 py-1 text-[12px] text-inactive-badge">
            Inactive
          </span>
        )}
      </td>
    </tr>
  )
}
