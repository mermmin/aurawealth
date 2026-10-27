const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })
const shortDate = new Intl.DateTimeFormat('en-US', { dateStyle: 'medium' })
const monthDay = new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric' })

/** `$1,234.56`, or `−$1,234.56` with a true minus sign so negatives line up with tabular figures. */
export function formatMoney(amount: number): string {
  const formatted = usd.format(Math.abs(amount))
  return amount < 0 && Math.round(amount * 100) !== 0 ? `−${formatted}` : formatted
}

/** `Oct 25, 2026` from an ISO instant or date. */
export function formatDate(iso: string): string {
  return shortDate.format(parseDate(iso))
}

/** `Sep 26 – Oct 25, 2026`; the year shows on both ends only when they differ. */
export function formatDateRange(fromIso: string, toIso: string): string {
  const from = parseDate(fromIso)
  const to = parseDate(toIso)
  const start = from.getFullYear() === to.getFullYear() ? monthDay.format(from) : shortDate.format(from)
  return `${start} – ${shortDate.format(to)}`
}

// `new Date('2026-10-25')` is UTC midnight, which is still Oct 24 west of Greenwich; read plain dates as local.
function parseDate(iso: string): Date {
  const plain = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso)
  return plain ? new Date(Number(plain[1]), Number(plain[2]) - 1, Number(plain[3])) : new Date(iso)
}
