import { Plus } from 'lucide-react'

/** Opens Plaid Link (Session 24); shown but not wired yet. */
export default function LinkBankButton() {
  return (
    <button
      type="button"
      disabled
      title="Linking a bank is coming soon"
      className="flex h-10 shrink-0 items-center gap-2 rounded-button bg-plum px-4 text-[15px] font-medium text-white hover:bg-plum/90 disabled:cursor-not-allowed disabled:opacity-70"
    >
      <Plus className="size-4" aria-hidden />
      Link a bank
    </button>
  )
}
