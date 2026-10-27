import { Info } from 'lucide-react'
import { institutionOf, isLiability, type Account } from '../../api/accounts'
import { formatMoney } from '../../lib/format'
import BankTile from '../BankTile'
import { Card, CardTitle } from '../Card'

// Within a bank: what you have first, then what you owe.
const TYPE_ORDER = ['depository', 'investment', 'credit', 'loan']
const OTHER_BANK = 'Other bank'

export default function AccountsTable({ accounts }: { accounts: Account[] }) {
  const groups = groupByBank(accounts)

  return (
    <Card>
      <CardTitle action={<span className="text-[13px] text-ink-muted">All balances in USD</span>}>
        Linked accounts
      </CardTitle>

      <div className="mt-6 overflow-x-auto">
        <table className="w-full min-w-[640px] text-left">
          <thead>
            <tr className="border-b border-border text-[12px] font-medium tracking-wider text-ink-muted uppercase">
              <th scope="col" className="px-5 pb-3 font-medium">Account</th>
              <th scope="col" className="px-5 pb-3 font-medium">Type / subtype</th>
              <th scope="col" className="px-5 pb-3 text-right font-medium">Current balance</th>
              <th scope="col" className="px-5 pb-3 text-right font-medium">Available balance</th>
            </tr>
          </thead>
          {groups.map(([bank, rows]) => (
            <tbody key={bank}>
              <tr className="border-b border-border bg-bg-subtle">
                <th scope="colgroup" colSpan={4} className="px-4 py-3 text-left font-normal">
                  <span className="flex items-center gap-3">
                    <BankTile institution={bank} />
                    <span className="text-[15px] font-medium text-plum">{bank}</span>
                    <span className="text-[13px] text-ink-muted">
                      {rows.length} {rows.length === 1 ? 'account' : 'accounts'}
                    </span>
                  </span>
                </th>
              </tr>
              {rows.map((account) => (
                <AccountRow key={account.id} account={account} />
              ))}
            </tbody>
          ))}
        </table>
      </div>

      <p className="mt-5 flex items-center gap-2 text-[13px] text-ink-muted">
        <Info className="size-4 shrink-0" aria-hidden />
        Available credit is not an asset. An em dash means the bank hasn’t provided an available balance.
      </p>
    </Card>
  )
}

function AccountRow({ account }: { account: Account }) {
  const owed = isLiability(account)

  return (
    <tr className="border-b border-border">
      <td className="px-5 py-4">
        <p className="text-[15px] text-ink">{account.name}</p>
        {account.mask && <p className="money mt-0.5 text-[13px] text-ink-muted">••••{account.mask}</p>}
      </td>
      <td className="px-5 py-4">
        <p className="text-[15px] text-ink">{capitalize(account.type)}</p>
        {account.subtype && <p className="mt-0.5 text-[13px] text-ink-muted">{account.subtype}</p>}
      </td>
      <td className="px-5 py-4 text-right">
        <Balance amount={account.currentBalance} caption={owed ? 'Amount owed' : undefined} strong />
      </td>
      <td className="px-5 py-4 text-right">
        <Balance amount={account.availableBalance} caption={owed ? 'Available credit' : undefined} />
      </td>
    </tr>
  )
}

function Balance({ amount, caption, strong }: { amount: number | null; caption?: string; strong?: boolean }) {
  if (amount === null) {
    return (
      <>
        <p className="text-[15px] text-ink" aria-hidden>—</p>
        <p className="mt-0.5 text-[13px] text-ink-muted">Not provided</p>
      </>
    )
  }
  return (
    <>
      <p className={`money text-[15px] text-ink ${strong ? 'font-medium' : ''}`}>{formatMoney(amount)}</p>
      {caption && <p className="mt-0.5 text-[13px] text-ink-muted">{caption}</p>}
    </>
  )
}

/** Banks A–Z with "Other bank" last; accounts by type, then name. */
function groupByBank(accounts: Account[]): [string, Account[]][] {
  const groups = new Map<string, Account[]>()
  for (const account of accounts) {
    const bank = institutionOf(account)
    groups.set(bank, [...(groups.get(bank) ?? []), account])
  }
  for (const rows of groups.values()) {
    rows.sort((a, b) => typeRank(a) - typeRank(b) || a.name.localeCompare(b.name))
  }
  return [...groups].sort(([a], [b]) => (a === OTHER_BANK ? 1 : b === OTHER_BANK ? -1 : a.localeCompare(b)))
}

function typeRank(account: Account): number {
  const rank = TYPE_ORDER.indexOf(account.type)
  return rank === -1 ? TYPE_ORDER.length : rank
}

function capitalize(word: string): string {
  return word.charAt(0).toUpperCase() + word.slice(1)
}
