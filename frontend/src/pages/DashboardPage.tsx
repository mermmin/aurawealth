import PageHeader from '../components/PageHeader'
import NetWorthCard from '../components/dashboard/NetWorthCard'
import AccountsCard from '../components/dashboard/AccountsCard'
import SpendingCard from '../components/dashboard/SpendingCard'
import OnRepeatCard from '../components/dashboard/OnRepeatCard'
import { SyncButton, SyncResultStrip } from '../components/dashboard/Sync'
import { useSync } from '../api/dashboard'
import { nameOf, useMe } from '../api/me'

export default function DashboardPage() {
  const { data: me } = useMe()
  const sync = useSync()
  const welcome = me ? `Welcome back, ${nameOf(me)}.` : 'Welcome back.'

  return (
    <>
      <PageHeader
        page="Dashboard"
        title="A little clarity. A lot of possibility."
        subtitle={`${welcome} Here’s where your money stands today.`}
        action={<SyncButton sync={sync} />}
      />
      <div className="space-y-7">
        <div className="grid gap-6 xl:grid-cols-[62fr_38fr]">
          <NetWorthCard />
          <AccountsCard />
        </div>
        <SyncResultStrip sync={sync} />
        <div className="grid gap-6 xl:grid-cols-[62fr_38fr]">
          <SpendingCard />
          <OnRepeatCard />
        </div>
      </div>
    </>
  )
}
