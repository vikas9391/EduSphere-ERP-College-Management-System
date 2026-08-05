// src/components/LeafDivider.tsx
//
// A real botanical illustration (not a procedural/SVG vine) used as a
// decorative seam between the sidebar and the page content, ported from
// the botanical theme reference. The art is narrow near the top (a single
// sprig) and fans out wide near the bottom, so the column it renders in is
// wider than it looks and overflow-visible, letting the bottom leaf cluster
// spill slightly over the sidebar/content edges without clipping. It's
// pointer-events-none so nav items and page content underneath stay fully
// clickable, and it's hidden below lg since there's no room for it once
// the sidebar itself goes off-canvas.
import vineIllustration from '@/assets/illustrations/sidebar-vine.webp'

export function LeafDivider() {
  return (
    <div
      className="pointer-events-none relative -ml-8 hidden h-[calc(100vh-2rem)] w-28 shrink-0 md:block"
      aria-hidden="true"
    >
      <img
        src={vineIllustration}
        alt=""
        draggable={false}
        className="absolute bottom-0 left-1/2 h-full w-[220px] max-w-none -translate-x-1/2 select-none object-cover object-bottom"
        style={{
          transform: 'translateX(-5%) rotate(5deg) skewY(-4deg)',
          transformOrigin: 'bottom center',
          WebkitMaskImage:
            'linear-gradient(to right, transparent 0%, black 14%, black 86%, transparent 100%), linear-gradient(to bottom, transparent 0%, black 5%, black 90%, transparent 100%)',
          WebkitMaskComposite: 'source-in, source-in',
          maskImage:
            'linear-gradient(to right, transparent 0%, black 14%, black 86%, transparent 100%), linear-gradient(to bottom, transparent 0%, black 5%, black 90%, transparent 100%)',
          maskComposite: 'intersect',
        }}
      />
    </div>
  )
}
