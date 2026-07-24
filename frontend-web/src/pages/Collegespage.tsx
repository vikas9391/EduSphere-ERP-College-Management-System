// src/pages/CollegesPage.tsx
import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Layout } from '@/components/Layout'
import { Field, inputClass } from '@/components/FormField'
import { StampGrid, LedgerRule } from '@/components/motion'
import { StatCard, PanelError, STAT_SHADES } from '@/components/PageBits'
import {
  registerTenant,
  listTenants,
  updateTenantStatus,
  updateTenantSubscription,
  deleteTenant,
  type TenantSummary,
} from '@/api/tenant'
import {
  Building2,
  Eye,
  EyeOff,
  Loader2,
  CheckCircle2,
  Copy,
  RefreshCw,
  Ban,
  PlayCircle,
  Trash2,
  Pencil,
  AlertTriangle,
  ShieldCheck,
  ShieldOff,
} from 'lucide-react'

const emptyForm = {
  collegeName: '',
  subdomain: '',
  adminEmail: '',
  password: '',
  confirmPassword: '',
}

const EASE_STAMP = [0.16, 1, 0.3, 1] as const

const panelIn = {
  hidden: { opacity: 0, y: 14 },
  show: (i: number) => ({
    opacity: 1,
    y: 0,
    transition: { duration: 0.4, ease: EASE_STAMP, delay: 0.06 * i },
  }),
}

const listStagger = {
  hidden: {},
  show: { transition: { staggerChildren: 0.05, delayChildren: 0.05 } },
}

const listItem = {
  hidden: { opacity: 0, x: -8 },
  show: { opacity: 1, x: 0, transition: { duration: 0.24, ease: EASE_STAMP } },
}

// Mirrors the backend's schema derivation:
// String schemaName = subdomain.toLowerCase().replaceAll("[^a-z0-9]", "_");
function previewSchemaName(subdomain: string) {
  return subdomain.toLowerCase().replace(/[^a-z0-9]/g, '_')
}

export function CollegesPage() {
  const [form, setForm] = useState(emptyForm)
  const [showPassword, setShowPassword] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [copiedId, setCopiedId] = useState<string | null>(null)

  const [tenants, setTenants] = useState<TenantSummary[]>([])
  const [loadingTenants, setLoadingTenants] = useState(true)
  const [listError, setListError] = useState<string | null>(null)

  // The admin email entered at registration time isn't stored on the tenant record
  // itself (it lives inside that college's own tenant-schema `users` table, which
  // this SUPER_ADMIN session has no access to) — so "Copy credentials" is only
  // available for colleges registered in this browser session, where we still have
  // the email in memory. Keyed by tenantId.
  const [sessionAdminEmails, setSessionAdminEmails] = useState<Record<string, string>>({})

  const schemaPreview = previewSchemaName(form.subdomain)

  const activeCount = tenants.filter((t) => t.isActive).length
  const suspendedCount = tenants.length - activeCount

  async function loadTenants() {
    setLoadingTenants(true)
    setListError(null)
    try {
      const result = await listTenants()
      setTenants(result)
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Could not load colleges.'
      setListError(message)
    } finally {
      setLoadingTenants(false)
    }
  }

  useEffect(() => {
    loadTenants()
  }, [])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)

    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match.')
      return
    }
    if (form.password.length < 8) {
      setError('Password must be at least 8 characters.')
      return
    }

    setSaving(true)
    try {
      const result = await registerTenant({
        collegeName: form.collegeName.trim(),
        subdomain: form.subdomain.trim().toLowerCase(),
        adminEmail: form.adminEmail.trim(),
        password: form.password,
      })
      setSessionAdminEmails((prev) => ({ ...prev, [result.tenantId]: form.adminEmail.trim() }))
      setForm(emptyForm)
      await loadTenants()
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Could not register this college. The subdomain may already be taken.'
      setError(message)
    } finally {
      setSaving(false)
    }
  }

  function copyCredentials(entry: TenantSummary) {
    const adminEmail = sessionAdminEmails[entry.tenantId]
    const text = `College: ${entry.collegeName}\nAdmin email: ${adminEmail}\nLogin: https://${entry.schemaName}.yourdomain.com`
    navigator.clipboard.writeText(text).then(() => {
      setCopiedId(entry.tenantId)
      setTimeout(() => setCopiedId(null), 1500)
    })
  }

  return (
    <Layout>
      <div className="mb-6">
        <h1 className="font-display text-2xl font-medium text-ink">Register a College</h1>
        <p className="mt-1 text-sm text-slate-dim">
          Provisions an isolated database schema and creates the college's first administrator account.
        </p>
      </div>

      <StampGrid className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard icon={Building2} label="Total Colleges" value={tenants.length} accent={STAT_SHADES[0]} />
        <StatCard icon={ShieldCheck} label="Active" value={activeCount} accent={STAT_SHADES[3]} />
        <StatCard icon={ShieldOff} label="Suspended" value={suspendedCount} accent={STAT_SHADES[6]} />
      </StampGrid>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-5">
        {/* Registration form */}
        <motion.div
          className="paper rounded-lg border border-parchment-line bg-white/60 p-6 shadow-[var(--shadow-paper-lift)] lg:col-span-3"
          custom={0}
          variants={panelIn}
          initial="hidden"
          animate="show"
        >
          <form onSubmit={handleSubmit} className="space-y-4">
            <h3 className="border-b border-parchment-line pb-2 font-display text-base font-medium text-ink">
              College Details
            </h3>

            <Field label="College Name">
              <input
                required
                autoFocus
                value={form.collegeName}
                onChange={(e) => setForm({ ...form, collegeName: e.target.value })}
                placeholder="e.g. St. Xavier's College"
                className={inputClass}
              />
            </Field>

            <Field label="Subdomain">
              <input
                required
                value={form.subdomain}
                onChange={(e) => setForm({ ...form, subdomain: e.target.value })}
                placeholder="e.g. st-xaviers"
                className={`${inputClass} font-mono`}
              />
              {form.subdomain && (
                <p className="mt-1 text-xs text-slate-dim">
                  Schema will be created as{' '}
                  <span className="font-mono text-ink">{schemaPreview || '—'}</span>
                </p>
              )}
            </Field>

            <h3 className="border-b border-parchment-line pb-2 pt-3 font-display text-base font-medium text-ink">
              First Administrator
            </h3>
            <p className="text-xs text-slate-dim">
              This account will be created for the college with the ADMIN role, ready to sign in immediately.
            </p>

            <Field label="Admin Email">
              <input
                required
                type="email"
                value={form.adminEmail}
                onChange={(e) => setForm({ ...form, adminEmail: e.target.value })}
                placeholder="admin@stxaviers.edu"
                className={inputClass}
              />
            </Field>

            <div className="grid grid-cols-2 gap-4">
              <Field label="Password">
                <div className="relative">
                  <input
                    required
                    type={showPassword ? 'text' : 'password'}
                    value={form.password}
                    onChange={(e) => setForm({ ...form, password: e.target.value })}
                    className={`${inputClass} pr-10`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((v) => !v)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate hover:text-ink"
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                  </button>
                </div>
              </Field>
              <Field label="Confirm Password">
                <input
                  required
                  type={showPassword ? 'text' : 'password'}
                  value={form.confirmPassword}
                  onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
                  className={inputClass}
                />
              </Field>
            </div>

            {error && <p className="text-sm text-brick">{error}</p>}

            <div className="flex justify-end pt-4">
              <button
                type="submit"
                disabled={saving}
                className="flex items-center gap-2 rounded-md bg-brass px-5 py-2 text-sm font-medium text-ink hover:bg-brass-bright disabled:opacity-60"
              >
                {saving && <Loader2 size={16} className="animate-spin" />}
                Register College
              </button>
            </div>
          </form>
        </motion.div>

        {/* All registered colleges, from the backend */}
        <motion.div
          className="paper rounded-lg border border-parchment-line bg-white/60 p-6 shadow-[var(--shadow-paper-lift)] lg:col-span-2"
          custom={1}
          variants={panelIn}
          initial="hidden"
          animate="show"
        >
          <div className="mb-1 flex items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <Building2 size={16} className="text-brass" />
              <h3 className="font-display text-base font-medium text-ink">All Colleges</h3>
            </div>
            <button
              type="button"
              onClick={loadTenants}
              disabled={loadingTenants}
              className="flex items-center gap-1 text-xs text-slate hover:text-ink disabled:opacity-60"
              aria-label="Refresh"
            >
              <RefreshCw size={12} className={loadingTenants ? 'animate-spin' : ''} />
              Refresh
            </button>
          </div>
          <LedgerRule className="mb-4" />

          {listError && <PanelError message={listError} />}

          {!listError && loadingTenants && tenants.length === 0 && (
            <div className="mt-6 flex items-center justify-center gap-2 text-sm text-slate-dim">
              <Loader2 size={16} className="animate-spin" />
              Loading colleges…
            </div>
          )}

          {!listError && !loadingTenants && tenants.length === 0 && (
            <p className="mt-4 text-sm text-slate-dim">
              No colleges registered yet. Colleges you register will appear here.
            </p>
          )}

          {tenants.length > 0 && (
            <motion.ul className="mt-4 space-y-3" variants={listStagger} initial="hidden" animate="show">
              {tenants.map((entry) => (
                <motion.li key={entry.tenantId} variants={listItem}>
                  <TenantRow
                    entry={entry}
                    adminEmail={sessionAdminEmails[entry.tenantId]}
                    copied={copiedId === entry.tenantId}
                    onCopy={() => copyCredentials(entry)}
                    onChanged={(updated) =>
                      setTenants((prev) => prev.map((t) => (t.tenantId === updated.tenantId ? updated : t)))
                    }
                    onDeleted={() => setTenants((prev) => prev.filter((t) => t.tenantId !== entry.tenantId))}
                  />
                </motion.li>
              ))}
            </motion.ul>
          )}
        </motion.div>
      </div>
    </Layout>
  )
}

const statusBadgeClass: Record<TenantSummary['subscriptionStatus'], string> = {
  ACTIVE: 'bg-green-100 text-green-700',
  EXPIRED: 'bg-amber-100 text-amber-700',
  CANCELLED: 'bg-slate-200 text-slate-dim',
}

function TenantRow({
  entry,
  adminEmail,
  copied,
  onCopy,
  onChanged,
  onDeleted,
}: {
  entry: TenantSummary
  adminEmail?: string
  copied: boolean
  onCopy: () => void
  onChanged: (updated: TenantSummary) => void
  onDeleted: () => void
}) {
  const [statusBusy, setStatusBusy] = useState(false)
  const [statusError, setStatusError] = useState<string | null>(null)

  const [editingSubscription, setEditingSubscription] = useState(false)
  const [subForm, setSubForm] = useState({
    plan: entry.subscriptionPlan,
    status: entry.subscriptionStatus,
    expiresAt: entry.subscriptionExpiresAt ? entry.subscriptionExpiresAt.slice(0, 10) : '',
  })
  const [subBusy, setSubBusy] = useState(false)
  const [subError, setSubError] = useState<string | null>(null)

  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [confirmText, setConfirmText] = useState('')
  const [deleteBusy, setDeleteBusy] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  async function toggleStatus() {
    setStatusBusy(true)
    setStatusError(null)
    try {
      const updated = await updateTenantStatus(entry.tenantId, !entry.isActive)
      onChanged(updated)
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Could not update this college\'s status.'
      setStatusError(message)
    } finally {
      setStatusBusy(false)
    }
  }

  async function saveSubscription(e: FormEvent) {
    e.preventDefault()
    setSubBusy(true)
    setSubError(null)
    try {
      const updated = await updateTenantSubscription(entry.tenantId, {
        plan: subForm.plan.trim(),
        status: subForm.status,
        expiresAt: subForm.expiresAt ? new Date(subForm.expiresAt).toISOString() : null,
      })
      onChanged(updated)
      setEditingSubscription(false)
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Could not update the subscription.'
      setSubError(message)
    } finally {
      setSubBusy(false)
    }
  }

  async function confirmDelete() {
    setDeleteBusy(true)
    setDeleteError(null)
    try {
      await deleteTenant(entry.tenantId)
      onDeleted()
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Could not delete this college.'
      setDeleteError(message)
      setDeleteBusy(false)
    }
  }

  return (
    <li className="rounded-md border border-parchment-line/70 p-3">
      <div className="flex items-start justify-between gap-2">
        {/* Navigates to the college's full detail view in this same tab, instead of
            expanding inline here. The whole info block is clickable, not just the name. */}
        <Link to={`/colleges/${entry.tenantId}`} className="block min-w-0 group">
          <div className="flex items-center gap-1.5 text-left text-sm font-medium text-ink group-hover:text-brass">
            <CheckCircle2
              size={14}
              className={`shrink-0 ${entry.isActive ? 'text-green-600' : 'text-slate'}`}
            />
            {entry.collegeName}
            {!entry.isActive && (
              <span className="rounded bg-slate-200 px-1.5 py-0.5 text-[10px] font-medium uppercase text-slate-dim">
                Suspended
              </span>
            )}
          </div>
          <p className="mt-1 truncate font-mono text-xs text-slate-dim">{entry.schemaName}</p>
          {adminEmail && <p className="truncate text-xs text-slate-dim">{adminEmail}</p>}

          <div className="mt-1.5 flex items-center gap-1.5 text-xs">
            <span className={`rounded px-1.5 py-0.5 font-medium ${statusBadgeClass[entry.subscriptionStatus]}`}>
              {entry.subscriptionPlan} · {entry.subscriptionStatus}
            </span>
            {entry.subscriptionExpiresAt && (
              <span className="text-slate-dim">
                exp. {new Date(entry.subscriptionExpiresAt).toLocaleDateString()}
              </span>
            )}
          </div>
        </Link>

        {adminEmail && (
          <button
            type="button"
            onClick={onCopy}
            className="flex shrink-0 items-center gap-1 text-xs text-slate hover:text-ink"
          >
            <Copy size={12} />
            {copied ? 'Copied' : 'Copy'}
          </button>
        )}
      </div>

      {/* Row actions */}
      <div className="mt-3 flex flex-wrap items-center gap-3 border-t border-parchment-line/70 pt-2">
        <button
          type="button"
          onClick={toggleStatus}
          disabled={statusBusy}
          className={`flex items-center gap-1 text-xs font-medium disabled:opacity-60 ${
            entry.isActive ? 'text-brick hover:text-brick' : 'text-green-700 hover:text-green-800'
          }`}
        >
          {statusBusy ? (
            <Loader2 size={12} className="animate-spin" />
          ) : entry.isActive ? (
            <Ban size={12} />
          ) : (
            <PlayCircle size={12} />
          )}
          {entry.isActive ? 'Suspend' : 'Reactivate'}
        </button>

        <button
          type="button"
          onClick={() => setEditingSubscription((v) => !v)}
          className="flex items-center gap-1 text-xs text-slate hover:text-ink"
        >
          <Pencil size={12} />
          Subscription
        </button>

        <button
          type="button"
          onClick={() => setConfirmingDelete(true)}
          className="ml-auto flex items-center gap-1 text-xs text-brick hover:text-brick"
        >
          <Trash2 size={12} />
          Delete
        </button>
      </div>

      {statusError && <p className="mt-2 text-xs text-brick">{statusError}</p>}

      {editingSubscription && (
        <form onSubmit={saveSubscription} className="mt-3 space-y-2 rounded-md bg-parchment/60 p-3">
          <div className="grid grid-cols-2 gap-2">
            <Field label="Plan">
              <input
                required
                value={subForm.plan}
                onChange={(e) => setSubForm({ ...subForm, plan: e.target.value })}
                placeholder="e.g. TRIAL, BASIC, PRO"
                className={inputClass}
              />
            </Field>
            <Field label="Status">
              <select
                value={subForm.status}
                onChange={(e) =>
                  setSubForm({ ...subForm, status: e.target.value as TenantSummary['subscriptionStatus'] })
                }
                className={inputClass}
              >
                <option value="ACTIVE">ACTIVE</option>
                <option value="EXPIRED">EXPIRED</option>
                <option value="CANCELLED">CANCELLED</option>
              </select>
            </Field>
          </div>
          <Field label="Expires on (optional)">
            <input
              type="date"
              value={subForm.expiresAt}
              onChange={(e) => setSubForm({ ...subForm, expiresAt: e.target.value })}
              className={inputClass}
            />
          </Field>
          {subError && <p className="text-xs text-brick">{subError}</p>}
          <div className="flex justify-end gap-2 pt-1">
            <button
              type="button"
              onClick={() => setEditingSubscription(false)}
              className="rounded-md px-3 py-1.5 text-xs text-slate hover:text-ink"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={subBusy}
              className="flex items-center gap-1 rounded-md bg-brass px-3 py-1.5 text-xs font-medium text-ink hover:bg-brass-bright disabled:opacity-60"
            >
              {subBusy && <Loader2 size={12} className="animate-spin" />}
              Save
            </button>
          </div>
        </form>
      )}

      {confirmingDelete && (
        <div className="mt-3 space-y-2 rounded-md border border-brick/40 bg-brick/5 p-3">
          <p className="flex items-center gap-1.5 text-xs font-medium text-brick">
            <AlertTriangle size={13} />
            This permanently deletes "{entry.collegeName}" and every student, teacher, and
            record it has. This cannot be undone.
          </p>
          <p className="text-xs text-slate-dim">
            Type <span className="font-mono text-ink">{entry.subdomain}</span> to confirm.
          </p>
          <input
            value={confirmText}
            onChange={(e) => setConfirmText(e.target.value)}
            className={`${inputClass} font-mono`}
            placeholder={entry.subdomain}
          />
          {deleteError && <p className="text-xs text-brick">{deleteError}</p>}
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => {
                setConfirmingDelete(false)
                setConfirmText('')
                setDeleteError(null)
              }}
              className="rounded-md px-3 py-1.5 text-xs text-slate hover:text-ink"
            >
              Cancel
            </button>
            <button
              type="button"
              disabled={confirmText.trim() !== entry.subdomain || deleteBusy}
              onClick={confirmDelete}
              className="flex items-center gap-1 rounded-md bg-brick px-3 py-1.5 text-xs font-medium text-white disabled:opacity-40"
            >
              {deleteBusy && <Loader2 size={12} className="animate-spin" />}
              Permanently delete
            </button>
          </div>
        </div>
      )}
    </li>
  )
}