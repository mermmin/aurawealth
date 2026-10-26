import type { ReactNode } from 'react'

export function Card({ className = '', children }: { className?: string; children: ReactNode }) {
  return <section className={`rounded-card border border-border bg-surface p-7 ${className}`}>{children}</section>
}

export function CardTitle({ children, action }: { children: ReactNode; action?: ReactNode }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <h2 className="font-display text-[32px] leading-tight text-ink">{children}</h2>
      {action}
    </div>
  )
}

/** Grey bars in the rough shape of the content, while it loads. */
export function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`animate-pulse rounded-tile bg-border/70 ${className}`} aria-hidden />
}

export function CardError({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div role="alert" className="flex items-center justify-between gap-4 rounded-button bg-blush px-4 py-3 text-[13px] text-plum">
      <span>{message}</span>
      <button type="button" onClick={onRetry} className="shrink-0 font-medium underline-offset-2 hover:underline">
        Try again
      </button>
    </div>
  )
}
