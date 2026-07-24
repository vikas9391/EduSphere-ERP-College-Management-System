export function SealMark() {
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
        stroke="var(--color-brass)"
        strokeWidth="1.5"
        className="seal-path"
        pathLength={100}
      />
      <circle
        cx="100"
        cy="100"
        r="68"
        stroke="var(--color-brass)"
        strokeWidth="1"
        className="seal-path"
        style={{ animationDelay: '0.15s' }}
        pathLength={100}
      />
      {/* Open book mark, simplified */}
      <path
        d="M100 78 C 88 70, 68 68, 58 74 L 58 128 C 68 122, 88 124, 100 132 C 112 124, 132 122, 142 128 L 142 74 C 132 68, 112 70, 100 78 Z"
        stroke="var(--color-brass)"
        strokeWidth="1.5"
        strokeLinejoin="round"
        className="seal-path"
        style={{ animationDelay: '0.3s' }}
        pathLength={100}
      />
      <line
        x1="100"
        y1="78"
        x2="100"
        y2="132"
        stroke="var(--color-brass)"
        strokeWidth="1.5"
        className="seal-path"
        style={{ animationDelay: '0.45s' }}
        pathLength={100}
      />
      {/* Tick marks around the rim, like a registry stamp */}
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
            stroke="var(--color-brass)"
            strokeWidth="1"
            className="seal-tick"
            style={{ animationDelay: `${0.5 + i * 0.012}s` }}
          />
        )
      })}
    </svg>
  )
}
