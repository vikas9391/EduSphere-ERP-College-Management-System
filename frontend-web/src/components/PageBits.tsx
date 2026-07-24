// src/components/PageBits.tsx
//
// Small presentational pieces shared across pages so every screen reads as
// part of the same ledger/parchment system instead of each page inventing
// its own stat-card or section-header markup. Lifted from the pattern
// established in StudentDashboard.tsx.
import { AlertTriangle } from 'lucide-react'
import { StampItem, TallyCounter, LedgerRule } from '@/components/motion'

export const COLORS = {
  brass: '#c9a227',
  brassBright: '#d8b74a',
  ink: '#2b2620',
  brick: '#b5533c',
  slate: '#6b6558',
  slateDim: '#8a8578',
  green: '#3f7d55',
  grid: '#e7ddc9',
}

// One tonal family (shades of brass) so a row of stat cards reads as a
// deliberate, cohesive set rather than a traffic-light mix of hues.
export const STAT_SHADES = [
  '#8a6a1c',
  '#a9812f',
  '#c9a227',
  '#b58f26',
  '#d8b74a',
  '#c2a04a',
  '#e2c66e',
  '#cfa93a',
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
    <StampItem className="stat-card relative rounded-lg border border-parchment-line bg-white px-5 pt-5 pb-7">
      <div className="flex items-center justify-between">
        <p className="text-sm text-slate-dim">{label}</p>
        <span
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md"
          style={{
            backgroundColor: (failed ? COLORS.brick : accent ?? COLORS.brass) + '22',
            color: failed ? COLORS.brick : accent ?? COLORS.brass,
          }}
        >
          {failed ? <AlertTriangle size={18} /> : <Icon size={18} />}
        </span>
      </div>
      {failed ? (
        <p className="mt-3 text-sm font-medium text-brick">Couldn't load</p>
      ) : (
        <p className="mt-3 text-3xl font-semibold text-ink">
          {typeof value === 'number' ? <TallyCounter value={value} /> : value}
          {suffix && <span className="ml-1 text-lg font-medium text-slate-dim">{suffix}</span>}
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
          <Icon size={16} className="text-brass" />
          <h2 className="font-display text-base font-medium text-ink">{title}</h2>
        </div>
        {action}
      </div>
      {note && <p className="mt-1 text-xs italic text-slate-dim">{note}</p>}
      <LedgerRule className="mt-3" />
    </div>
  )
}

/** Shown inside a panel body when its backing request failed, distinct from
 *  a genuine "nothing here yet" empty state. */
export function PanelError({ message = "Couldn't load this section. Try refreshing the page." }: { message?: string }) {
  return (
    <div className="flex items-start gap-2 text-sm text-brick">
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
  success: COLORS.green,
  warning: COLORS.brass,
  danger: COLORS.brick,
  neutral: COLORS.slate,
}

/**
 * A small rounded status/category pill, tinted with the same registrar
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