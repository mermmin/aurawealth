import PageHeader from '../components/PageHeader'
import { nameOf, useMe } from '../api/me'

export default function DashboardPage() {
  const { data: me } = useMe()
  const welcome = me ? `Welcome back, ${nameOf(me)}.` : 'Welcome back.'

  return (
    <PageHeader
      page="Dashboard"
      title="A little clarity. A lot of possibility."
      subtitle={`${welcome} Here’s where your money stands today.`}
    />
  )
}
