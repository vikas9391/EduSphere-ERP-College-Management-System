// src/components/FormField.tsx
import type { ReactNode } from 'react'

export const inputClass =
  'w-full rounded-md border border-parchment-line bg-white/60 px-3.5 py-2 text-sm text-ink focus:border-brass focus:outline-none focus:ring-1 focus:ring-brass'

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
      <label className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-slate-dim">
        {label}
      </label>
      {children}
    </div>
  )
}