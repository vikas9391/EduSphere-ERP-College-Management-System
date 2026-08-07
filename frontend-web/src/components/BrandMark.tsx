// src/components/BrandMark.tsx
//
// Small geometric "N" monogram used next to the wordmark on the login
// screen. Two overlapping strokes so it reads as a single mark at small
// sizes without needing an image asset. Colors default to the login
// page's brand greens but can be overridden by other callers.
export function BrandMark({
  className = 'h-10 w-10',
  bg = '#ECFDF3',
  stroke = '#15803D',
  accent = '#22C55E',
}: {
  className?: string
  bg?: string
  stroke?: string
  accent?: string
}) {
  return (
    <svg
      viewBox="0 0 40 40"
      className={className}
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <rect width="40" height="40" rx="11" fill={bg} />
      <path
        d="M13 29V11L27 29V11"
        stroke={stroke}
        strokeWidth="4.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="27" cy="11" r="3" fill={accent} />
    </svg>
  )
}
