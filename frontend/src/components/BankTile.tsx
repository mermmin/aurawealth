// Bank tiles from §9; no brand colors in v1, so each bank gets a stable pick from this list.
const TILES = [
  'bg-[#E6EDF5] text-[#35618A]',
  'bg-[#F0E7F0] text-[#512C48]',
  'bg-[#E8EEE5] text-[#4E7047]',
]

function tileFor(name: string): string {
  let hash = 0
  for (const char of name) hash = (hash * 31 + char.charCodeAt(0)) >>> 0
  return TILES[hash % TILES.length]
}

export default function BankTile({ institution }: { institution: string }) {
  return (
    <span
      className={`flex size-10 shrink-0 items-center justify-center rounded-tile text-xl font-medium ${tileFor(institution)}`}
      aria-hidden
    >
      {institution.charAt(0)}
    </span>
  )
}
