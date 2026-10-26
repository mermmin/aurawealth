import { CircleAlert, CircleCheck, RefreshCw } from 'lucide-react'
import type { useSync } from '../../api/dashboard'

type SyncMutation = ReturnType<typeof useSync>

export function SyncButton({ sync }: { sync: SyncMutation }) {
  return (
    <button
      type="button"
      onClick={() => sync.mutate()}
      disabled={sync.isPending}
      className="flex h-10 shrink-0 items-center gap-2 rounded-button bg-plum px-4 text-[15px] font-medium text-white hover:bg-plum/90 disabled:opacity-70"
    >
      <RefreshCw className={`size-4 ${sync.isPending ? 'animate-spin' : ''}`} aria-hidden />
      {sync.isPending ? 'Syncing…' : 'Sync'}
    </button>
  )
}

/** One-line result of the last sync; nothing until the first one finishes. */
export function SyncResultStrip({ sync }: { sync: SyncMutation }) {
  if (sync.isError) {
    return (
      <div role="alert" className="flex items-center gap-3 rounded-button border border-border-blush bg-blush px-5 py-3 text-[13px] text-plum">
        <CircleAlert className="size-4 shrink-0" aria-hidden />
        <span className="font-medium">Sync didn’t finish.</span>
        <span>{sync.error.message}. Your last synced numbers are still shown.</span>
      </div>
    )
  }
  if (!sync.isSuccess) return null

  const { added, modified, removed } = sync.data
  return (
    <div role="status" className="flex items-center gap-3 rounded-button border border-border bg-surface px-5 py-3 text-[13px]">
      <CircleCheck className="size-4 shrink-0 text-success" aria-hidden />
      <span className="font-medium text-ink">Synced just now</span>
      <span className="money text-ink-muted">
        {added} added · {modified} updated · {removed} removed
      </span>
    </div>
  )
}
