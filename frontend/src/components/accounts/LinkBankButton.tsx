import { CircleAlert, LoaderCircle, Plus } from 'lucide-react'
import type { LinkBank } from '../../api/plaid'

const LABELS = { idle: 'Link a bank', opening: 'Opening Plaid…', linking: 'Linking…' }

export default function LinkBankButton({ link }: { link: LinkBank }) {
  const busy = link.phase !== 'idle'
  const Icon = busy ? LoaderCircle : Plus

  return (
    <button
      type="button"
      onClick={link.begin}
      disabled={busy}
      className="flex h-10 shrink-0 items-center gap-2 rounded-button bg-plum px-4 text-[15px] font-medium text-white hover:bg-plum/90 disabled:opacity-70"
    >
      <Icon className={`size-4 ${busy ? 'animate-spin' : ''}`} aria-hidden />
      {LABELS[link.phase]}
    </button>
  )
}

/** Why the last attempt didn't link; nothing otherwise. */
export function LinkBankError({ link }: { link: LinkBank }) {
  if (!link.error) return null
  return (
    <div role="alert" className="mb-7 flex items-center gap-3 rounded-button border border-border-blush bg-blush px-5 py-3 text-[13px] text-plum">
      <CircleAlert className="size-4 shrink-0" aria-hidden />
      <span>{link.error}</span>
    </div>
  )
}
