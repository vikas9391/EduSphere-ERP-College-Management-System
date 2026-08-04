// src/components/motion.tsx
//
// The app's motion vocabulary, kept in one place so every page moves the
// same way instead of each screen inventing its own transitions.
//
// Signature idea (2026 botanical refresh): things settle in with a gentle
// rise, the way a leaf drifts down to rest — no overshoot, no rotation.
// Lists arrive staggered rather than popping in together, and numbers count
// up smoothly instead of snapping to their value. Motion is quick
// (180–380ms) and always respects prefers-reduced-motion via Framer's
// built-in handling.
//
// Function names are kept stable across the old "registrar" theme and this
// one (PageIn, StampGrid, StampItem, TallyCounter, LedgerRule, ActiveRail)
// so pages can be migrated to the new visual system one at a time without
// having to change their imports.
import { motion, useReducedMotion, useSpring, useTransform, type Variants } from 'framer-motion'
import { useEffect, type ReactNode } from 'react'
import { Link, type LinkProps } from 'react-router-dom'

/** A React Router Link that's also a motion component (for whileHover/whileTap/variants). */
const MotionLink = motion.create(Link)

const EASE_SPROUT = [0.16, 1, 0.3, 1] as const

/** Wraps a page's content. Fade + a small upward settle. */
export function PageIn({ children, className }: { children: ReactNode; className?: string }) {
  const reduce = useReducedMotion()
  return (
    <motion.div
      className={className}
      initial={reduce ? false : { opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.32, ease: EASE_SPROUT }}
    >
      {children}
    </motion.div>
  )
}

const staggerContainer: Variants = {
  hidden: {},
  show: {
    transition: { staggerChildren: 0.055, delayChildren: 0.04 },
  },
}

const sproutItem: Variants = {
  hidden: { opacity: 0, y: 14 },
  show: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.36, ease: EASE_SPROUT },
  },
}

/** A grid/list of cards that settle in one after another rather than popping in together. */
export function StampGrid({ children, className }: { children: ReactNode; className?: string }) {
  const reduce = useReducedMotion()
  if (reduce) return <div className={className}>{children}</div>
  return (
    <motion.div className={className} variants={staggerContainer} initial="hidden" animate="show">
      {children}
    </motion.div>
  )
}

/** One entry in a StampGrid. A soft leaf-card that lifts slightly on hover. */
export function StampItem({
  children,
  className,
  to,
}: {
  children: ReactNode
  className?: string
  /** If provided, renders as a router Link to this path instead of a plain div. */
  to?: LinkProps['to']
}) {
  const shared = {
    className: `leaf-card leaf-card-interactive ${className ?? ''}`,
    variants: sproutItem,
    whileHover: { y: -4 },
    whileTap: { scale: 0.98 },
  }
  if (to) {
    return (
      <MotionLink to={to} {...shared}>
        {children}
      </MotionLink>
    )
  }
  return <motion.div {...shared}>{children}</motion.div>
}

/**
 * A stat number that counts up to its value instead of snapping straight to
 * it. Pass the raw numeric value.
 */
export function TallyCounter({
  value,
  className,
  format = (n) => Math.round(n).toLocaleString(),
}: {
  value: number
  className?: string
  format?: (n: number) => string
}) {
  const spring = useSpring(0, { stiffness: 90, damping: 20, mass: 0.6 })
  const display = useTransform(spring, (v) => format(v))

  useEffect(() => {
    spring.set(value)
  }, [value, spring])

  return <motion.span className={className}>{display}</motion.span>
}

/** A section divider that draws itself left-to-right on mount. */
export function LedgerRule({ className }: { className?: string }) {
  const reduce = useReducedMotion()
  return (
    <motion.div
      className={`h-px bg-primary/25 ${className ?? ''}`}
      style={{ transformOrigin: 'left' }}
      initial={reduce ? false : { scaleX: 0 }}
      animate={{ scaleX: 1 }}
      transition={{ duration: 0.5, ease: EASE_SPROUT, delay: 0.1 }}
    />
  )
}

/** The active-nav indicator in the sidebar — a green rule that glides between items via layoutId. */
export function ActiveRail({ layoutId = 'nav-active-rail' }: { layoutId?: string }) {
  return (
    <motion.span
      layoutId={layoutId}
      className="absolute inset-y-1 left-0 w-0.5 rounded-full bg-primary"
      transition={{ type: 'spring', stiffness: 500, damping: 40 }}
    />
  )
}
