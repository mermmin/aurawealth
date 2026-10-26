const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })
const shortDate = new Intl.DateTimeFormat('en-US', { dateStyle: 'medium' })

/** `$1,234.56`, or `−$1,234.56` with a true minus sign so negatives line up with tabular figures. */
export function formatMoney(amount: number): string {
  const formatted = usd.format(Math.abs(amount))
  return amount < 0 && Math.round(amount * 100) !== 0 ? `−${formatted}` : formatted
}

/** `Oct 25, 2026` from an ISO instant or date. */
export function formatDate(iso: string): string {
  return shortDate.format(new Date(iso))
}
