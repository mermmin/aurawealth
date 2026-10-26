import type { ReactNode } from 'react'
import { ChevronRight } from 'lucide-react'

const today = new Intl.DateTimeFormat('en-US', { dateStyle: 'long' })

interface Props {
  page: string
  title: string
  subtitle: ReactNode
  action?: ReactNode
}

export default function PageHeader({ page, title, subtitle, action }: Props) {
  return (
    <header className="mb-8">
      <div className="flex items-center justify-between text-[13px]">
        <nav aria-label="Breadcrumb" className="flex items-center gap-2">
          <span className="text-ink-muted">Your money</span>
          <ChevronRight className="size-3.5 text-ink-muted" aria-hidden />
          <span className="text-ink" aria-current="page">
            {page}
          </span>
        </nav>
        <span className="text-ink-muted">{today.format(new Date())}</span>
      </div>
      <div className="mt-6 flex items-end justify-between gap-6">
        <div>
          <h1 className="font-display text-5xl leading-tight text-ink">{title}</h1>
          <p className="mt-1 text-[15px] text-ink-muted">{subtitle}</p>
        </div>
        {action}
      </div>
    </header>
  )
}
