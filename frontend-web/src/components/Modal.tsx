// src/components/Modal.tsx
import type { ReactNode } from 'react'
import { X } from 'lucide-react'

export function Modal({
  title,
  onClose,
  children,
}: {
  title: string
  onClose: () => void
  children: ReactNode
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink/40 px-4">
      <div className="flex max-h-[90vh] w-full max-w-lg flex-col rounded-lg border border-parchment-line bg-parchment shadow-xl">
        <div className="flex items-center justify-between border-b border-parchment-line px-6 py-4">
          <h2 className="font-display text-lg font-medium text-ink">{title}</h2>
          <button onClick={onClose} className="text-slate hover:text-ink">
            <X size={18} />
          </button>
        </div>
        <div className="overflow-y-auto p-6">{children}</div>
      </div>
    </div>
  )
}