// src/components/PageBits.tsx
//
// Small presentational pieces shared across pages so every screen reads as
// part of the same botanical design system instead of each page inventing
// its own leaf-card or section-header markup.
import { AlertTriangle } from 'lucide-react'
import { StampItem, TallyCounter, LedgerRule } from '@/components/motion'

export const COLORS = {
  primary: '#2e7d32',
  secondary: '#4caf50',
  text: '#1f2937',
  danger: '#c1543c',
  muted: '#6b7280',
  green: '#3f7d55',
  grid: '#eef2e7',
}

// One tonal family (shades of green) so a row of stat cards reads as a
// deliberate, cohesive set rather than a traffic-light mix of hues.
export const STAT_SHADES = [
  '#1b5e20',
  '#2e7d32',
  '#388e3c',
  '#43a047',
  '#4caf50',
  '#66bb6a',
  '#81c784',
  '#2e7d32',
]

export function StatCard({
  icon: Icon,
  label,
  value,
  suffix,
  accent,
  failed,
}: {
  icon: React.ComponentType<{ size?: number | string; className?: string }>
  label: string
  value: string | number
  suffix?: string
  accent?: string
  /** When true, shows a "couldn't load" state instead of the value - used
   *  when the backing request failed rather than genuinely returning zero. */
  failed?: boolean
}) {
  return (
    <StampItem className="relative px-5 pt-5 pb-6">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted">{label}</p>
        <span
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl"
          style={{
            backgroundColor: (failed ? COLORS.danger : accent ?? COLORS.primary) + '1f',
            color: failed ? COLORS.danger : accent ?? COLORS.primary,
          }}
        >
          {failed ? <AlertTriangle size={18} /> : <Icon size={18} />}
        </span>
      </div>
      {failed ? (
        <p className="mt-3 text-sm font-medium text-danger">Couldn't load</p>
      ) : (
        <p className="mt-3 font-numbers text-3xl font-bold text-text">
          {typeof value === 'number' ? <TallyCounter value={value} /> : value}
          {suffix && <span className="ml-1 text-lg font-medium text-muted">{suffix}</span>}
        </p>
      )}
    </StampItem>
  )
}

export function PanelHeader({
  icon: Icon,
  title,
  note,
  action,
}: {
  icon: React.ComponentType<{ size?: number | string; className?: string }>
  title: string
  note?: string
  action?: React.ReactNode
}) {
  return (
    <div className="mb-4 pb-3">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Icon size={16} className="text-primary" />
          <h2 className="font-heading text-base font-semibold text-text">{title}</h2>
        </div>
        {action}
      </div>
      {note && <p className="mt-1 text-xs text-muted">{note}</p>}
      <LedgerRule className="mt-3" />
    </div>
  )
}

/** Shown inside a panel body when its backing request failed, distinct from
 *  a genuine "nothing here yet" empty state. */
export function PanelError({ message = "Couldn't load this section. Try refreshing the page." }: { message?: string }) {
  return (
    <div className="flex items-start gap-2 text-sm text-danger">
      <AlertTriangle size={16} className="mt-0.5 shrink-0" />
      <p>{message}</p>
    </div>
  )
}

/* ------------------------------------------------------------------ */
/* Badge                                                               */
/* ------------------------------------------------------------------ */

export type BadgeVariant = 'success' | 'warning' | 'danger' | 'neutral'

const BADGE_VARIANT_COLORS: Record<BadgeVariant, string> = {
  success: COLORS.primary,
  warning: COLORS.secondary,
  danger: COLORS.danger,
  neutral: COLORS.muted,
}

/**
 * A small rounded status/category pill, tinted with the same botanical
 * palette as everything else (rather than raw Tailwind semantic colors
 * like bg-green-100/text-green-700).
 *
 * Use `variant` for semantic meaning (status: success/warning/danger/neutral).
 * Use `color` for purely categorical distinctions (e.g. gender, semester)
 * where there's no "good/bad" meaning - it overrides variant.
 */
export function Badge({
  children,
  variant,
  color,
}: {
  children: React.ReactNode
  variant?: BadgeVariant
  color?: string
}) {
  const accent = color ?? BADGE_VARIANT_COLORS[variant ?? 'neutral']
  return (
    <span
      className="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium"
      style={{ backgroundColor: `${accent}1a`, color: accent }}
    >
      {children}
    </span>
  )
}
