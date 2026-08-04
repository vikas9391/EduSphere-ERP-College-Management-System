// src/components/LeafMark.tsx
//
// Botanical-theme replacement for the old wax-seal SVG: two rings drawing
// themselves in, a simple sprouting-leaf glyph at the center, and a ring of
// tick marks — same "drawing itself in" animation as before, new motif.
export function LeafMark() {
  return (
    <svg
      viewBox="0 0 200 200"
      className="h-40 w-40"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <circle
        cx="100"
        cy="100"
        r="82"
        stroke="var(--color-secondary)"
        strokeWidth="1.5"
        className="seal-path"
        pathLength={100}
      />
      <circle
        cx="100"
        cy="100"
        r="68"
        stroke="var(--color-secondary)"
        strokeWidth="1"
        className="seal-path"
        style={{ animationDelay: '0.15s' }}
        pathLength={100}
      />
      {/* Sprouting leaf, simplified */}
      <path
        d="M100 132 C 100 108, 100 92, 100 70"
        stroke="var(--color-primary)"
        strokeWidth="2"
        strokeLinecap="round"
        className="seal-path"
        style={{ animationDelay: '0.3s' }}
        pathLength={100}
      />
      <path
        d="M100 100 C 82 100, 68 88, 64 68 C 86 70, 98 82, 100 100 Z"
        stroke="var(--color-primary)"
        strokeWidth="1.5"
        strokeLinejoin="round"
        className="seal-path"
        style={{ animationDelay: '0.45s' }}
        pathLength={100}
      />
      <path
        d="M100 114 C 118 114, 132 102, 136 82 C 114 84, 102 96, 100 114 Z"
        stroke="var(--color-primary)"
        strokeWidth="1.5"
        strokeLinejoin="round"
        className="seal-path"
        style={{ animationDelay: '0.6s' }}
        pathLength={100}
      />
      {/* Tick marks around the rim */}
      {Array.from({ length: 24 }).map((_, i) => {
        const angle = (i / 24) * Math.PI * 2
        const x1 = 100 + Math.cos(angle) * 90
        const y1 = 100 + Math.sin(angle) * 90
        const x2 = 100 + Math.cos(angle) * 95
        const y2 = 100 + Math.sin(angle) * 95
        return (
          <line
            key={i}
            x1={x1}
            y1={y1}
            x2={x2}
            y2={y2}
            stroke="var(--color-secondary)"
            strokeWidth="1"
            className="seal-tick"
            style={{ animationDelay: `${0.75 + i * 0.012}s` }}
          />
        )
      })}
    </svg>
  )
}
