// src/components/FormField.tsx
import type { ReactNode } from 'react'

export const inputClass =
  'w-full rounded-[var(--radius-input)] border border-border bg-white px-3.5 py-2 text-sm text-text focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary'

export function Field({
  label,
  children,
  className,
}: {
  label: string
  children: ReactNode
  className?: string
}) {
  return (
    <div className={className}>
      <label className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-muted">
        {label}
      </label>
      {children}
    </div>
  )
}