import { NavLink } from 'react-router-dom'
import { Flower, Landmark, LayoutGrid, Repeat, Sprout, UserRound, type LucideIcon } from 'lucide-react'
import { nameOf, useMe } from '../api/me'

const NAV: { to: string; label: string; icon: LucideIcon }[] = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutGrid },
  { to: '/accounts', label: 'Accounts', icon: Landmark },
  { to: '/subscriptions', label: 'Subscriptions', icon: Repeat },
]

export default function Sidebar() {
  return (
    <aside className="sticky top-0 flex h-screen w-56 shrink-0 flex-col border-r border-border bg-bg-subtle px-5 pt-6 pb-5">
      <div className="flex items-center gap-2 px-1 text-plum">
        <Flower className="size-6" strokeWidth={1.5} aria-hidden />
        <span className="font-display text-3xl leading-none">aurawealth</span>
        <span className="size-1.5 rounded-full bg-pink" aria-hidden />
      </div>

      <nav className="mt-10" aria-label="Main">
        <p className="mb-2 px-1 text-xs font-medium tracking-wide text-ink-muted uppercase">
          Your money
        </p>
        <ul className="space-y-1.5">
          {NAV.map(({ to, label, icon: Icon }) => (
            <li key={to}>
              <NavLink
                to={to}
                className={({ isActive }) =>
                  `flex h-11 items-center gap-3 rounded-nav px-3 text-[15px] ${
                    isActive ? 'bg-blush font-medium text-plum' : 'text-ink-muted hover:text-ink'
                  }`
                }
              >
                {({ isActive }) => (
                  <>
                    <Icon className="size-5" strokeWidth={1.5} aria-hidden />
                    <span className="flex-1">{label}</span>
                    {isActive && <span className="size-1.5 rounded-full bg-plum" aria-hidden />}
                  </>
                )}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      <div className="mt-auto rounded-card bg-blush p-4">
        <Sprout className="size-5 text-plum" strokeWidth={1.5} aria-hidden />
        <p className="mt-3 font-display text-3xl leading-[1.1] text-ink">
          Small money moves. More room to grow.
        </p>
        <p className="mt-3 text-[13px] leading-5 text-ink">A little clarity for your everyday.</p>
      </div>

      <UserBlock />
    </aside>
  )
}

function UserBlock() {
  const { data: me } = useMe()
  const name = me ? nameOf(me) : null

  return (
    <div className="mt-5 flex items-center gap-3 border-t border-border pt-4">
      <span
        className="flex size-11 shrink-0 items-center justify-center rounded-full bg-blush font-display text-xl text-plum"
        aria-hidden
      >
        {name ? initials(name) : <UserRound className="size-5" strokeWidth={1.5} />}
      </span>
      <div className="min-w-0">
        <p className="truncate text-[15px] font-medium text-ink">{name ?? 'Not signed in'}</p>
        <p className="truncate text-[13px] text-ink-muted">{me ? me.email : 'Sign in to continue'}</p>
      </div>
    </div>
  )
}

function initials(name: string): string {
  return name
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('')
}
