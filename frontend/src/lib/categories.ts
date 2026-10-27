import {
  Car,
  Clapperboard,
  Coffee,
  HeartPulse,
  Hammer,
  House,
  Landmark,
  Plane,
  Receipt,
  Repeat,
  ShoppingBag,
  Sparkles,
  Wrench,
  type LucideIcon,
} from 'lucide-react'

/** Plaid's primary categories in plain words. v1 stores only the primary, so these stay broad. */
const LABELS: Record<string, string> = {
  RENT_AND_UTILITIES: 'Home & bills',
  FOOD_AND_DRINK: 'Food & coffee',
  GENERAL_MERCHANDISE: 'Shopping',
  TRANSPORTATION: 'Transport',
  TRAVEL: 'Travel',
  PERSONAL_CARE: 'Personal care',
  ENTERTAINMENT: 'Entertainment',
  GENERAL_SERVICES: 'Services',
  HOME_IMPROVEMENT: 'Home improvement',
  MEDICAL: 'Health',
  GOVERNMENT_AND_NON_PROFIT: 'Government & giving',
  BANK_FEES: 'Bank fees',
  UNCATEGORIZED: 'Uncategorized',
}

const ICONS: Record<string, LucideIcon> = {
  RENT_AND_UTILITIES: House,
  FOOD_AND_DRINK: Coffee,
  GENERAL_MERCHANDISE: ShoppingBag,
  TRANSPORTATION: Car,
  TRAVEL: Plane,
  PERSONAL_CARE: Sparkles,
  ENTERTAINMENT: Clapperboard,
  GENERAL_SERVICES: Wrench,
  HOME_IMPROVEMENT: Hammer,
  MEDICAL: HeartPulse,
  GOVERNMENT_AND_NON_PROFIT: Landmark,
  BANK_FEES: Receipt,
}

/** `FOOD_AND_DRINK` → "Food & coffee"; anything unmapped → title case ("SOME_NEW_THING" → "Some new thing"). */
export function categoryLabel(category: string | null): string {
  if (!category) return LABELS.UNCATEGORIZED
  const known = LABELS[category]
  if (known) return known
  const words = category.toLowerCase().replace(/_/g, ' ')
  return words.charAt(0).toUpperCase() + words.slice(1)
}

/** A generic icon for the category; v1 has no merchant logos. */
export function categoryIcon(category: string | null): LucideIcon {
  return (category && ICONS[category]) || Repeat
}
