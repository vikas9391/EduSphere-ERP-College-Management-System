// src/pages/CollegeDetailPage.tsx
import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { useParams, Link } from 'react-router-dom'
import { Layout } from '@/components/Layout'
import { Field, inputClass } from '@/components/FormField'
import { StampGrid } from '@/components/motion'
import { PanelHeader, PanelError } from '@/components/PageBits'
import {
  getTenantDetails,
  updateTenantStatus,
  updateTenantSubscription,
  deleteTenant,
  type TenantDetails,
  type TenantSummary,
} from '@/api/tenant'
import {
  ArrowLeft,
  Building2,
  Users,
  GraduationCap,
  Landmark,
  BookOpen,
  Layers,
  ClipboardList,
  Loader2,
  Ban,
  PlayCircle,
  Pencil,
  Trash2,
  AlertTriangle,
  Settings2,
} from 'lucide-react'

const statusBadgeClass: Record<TenantSummary['subscriptionStatus'], string> = {
  ACTIVE: 'bg-green-100 text-green-700',
  EXPIRED: 'bg-amber-100 text-amber-700',
  CANCELLED: 'bg-slate-200 text-slate-dim',
}

export function CollegeDetailPage() {
  const { tenantId } = useParams<{ tenantId: string }>()

  const [details, setDetails] = useState<TenantDetails | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [statusBusy, setStatusBusy] = useState(false)
  const [statusError, setStatusError] = useState<string | null>(null)

  const [editingSubscription, setEditingSubscription] = useState(false)
  const [subForm, setSubForm] = useState({ plan: '', status: 'ACTIVE' as TenantSummary['subscriptionStatus'], expiresAt: '' })
  const [subBusy, setSubBusy] = useState(false)
  const [subError, setSubError] = useState<string | null>(null)

  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [confirmText, setConfirmText] = useState('')
  const [deleteBusy, setDeleteBusy] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [deleted, setDeleted] = useState(false)

  async function loadDetails() {
    if (!tenantId) return
    setLoading(true)
    setLoadError(null)
    try {
      const result = await getTenantDetails(tenantId)
      setDetails(result)
      setSubForm({
        plan: result.college.subscriptionPlan,
        status: result.college.subscriptionStatus,
        expiresAt: result.college.subscriptionExpiresAt ? result.college.subscriptionExpiresAt.slice(0, 10) : '',
      })
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        "Could not load this college's details."
      setLoadError(message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadDetails()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tenantId])

  async function toggleStatus() {
    if (!details) return
    setStatusBusy(true)
    setStatusError(null)
    try {
      const updated = await updateTenantStatus(details.college.tenantId, !details.college.isActive)
      setDetails({ ...details, college: updated })
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        "Could not update this college's status."
      setStatusError(message)
    } finally {
      setStatusBusy(false)
    }
  }

  async function saveSubscription(e: FormEvent) {
    e.preventDefault()
    if (!details) return
    setSubBusy(true)
    setSubError(null)
    try {
      const updated = await updateTenantSubscription(details.college.tenantId, {
        plan: subForm.plan.trim(),
        status: subForm.status,
        expiresAt: subForm.expiresAt ? new Date(subForm.expiresAt).toISOString() : null,
      })
      setDetails({ ...details, college: updated })
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
    if (!details) return
    setDeleteBusy(true)
    setDeleteError(null)
    try {
      await deleteTenant(details.college.tenantId)
      setDeleted(true)
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Could not delete this college.'
      setDeleteError(message)
      setDeleteBusy(false)
    }
  }

  return (
    <Layout>
      <div className="mb-6 flex items-center gap-3">
        <Link
          to="/colleges"
          className="flex items-center gap-1 text-sm text-slate hover:text-ink"
        >
          <ArrowLeft size={15} />
          All Colleges
        </Link>
      </div>

      {loading && (
        <div className="flex items-center justify-center gap-2 py-16 text-sm text-slate-dim">
          <Loader2 size={18} className="animate-spin" />
          Loading college…
        </div>
      )}

      {!loading && loadError && <PanelError message={loadError} />}

      {!loading && deleted && (
        <div className="rounded-lg border border-parchment-line bg-white/60 p-6">
          <p className="text-sm text-ink">This college has been permanently deleted.</p>
          <Link to="/colleges" className="mt-2 inline-block text-sm text-brass hover:text-brass-bright">
            Back to All Colleges
          </Link>
        </div>
      )}

      {!loading && !loadError && !deleted && details && (
        <>
          <div className="mb-6 flex items-start justify-between gap-2">
            <div>
              <div className="flex items-center gap-2">
                <Building2 size={18} className="text-brass" />
                <h1 className="font-display text-2xl font-medium text-ink">{details.college.collegeName}</h1>
                {!details.college.isActive && (
                  <span className="rounded bg-slate-200 px-1.5 py-0.5 text-[10px] font-medium uppercase text-slate-dim">
                    Suspended
                  </span>
                )}
              </div>
              <p className="mt-1 font-mono text-sm text-slate-dim">{details.college.schemaName}</p>
              <div className="mt-2 flex items-center gap-1.5 text-xs">
                <span className={`rounded px-1.5 py-0.5 font-medium ${statusBadgeClass[details.college.subscriptionStatus]}`}>
                  {details.college.subscriptionPlan} · {details.college.subscriptionStatus}
                </span>
                {details.college.subscriptionExpiresAt && (
                  <span className="text-slate-dim">
                    exp. {new Date(details.college.subscriptionExpiresAt).toLocaleDateString()}
                  </span>
                )}
              </div>
            </div>
          </div>

          <StampGrid className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-7">
            <StatTile icon={<Users size={16} />} label="Admin / Staff" value={details.adminStaffCount} />
            <StatTile icon={<GraduationCap size={16} />} label="Teachers" value={details.teacherCount} />
            <StatTile icon={<Users size={16} />} label="Students" value={details.studentCount} />
            <StatTile icon={<Landmark size={16} />} label="Departments" value={details.departmentCount} />
            <StatTile icon={<BookOpen size={16} />} label="Courses" value={details.courseCount} />
            <StatTile icon={<Layers size={16} />} label="Subjects" value={details.subjectCount} />
            <StatTile icon={<ClipboardList size={16} />} label="Enrollments" value={details.enrollmentCount} />
          </StampGrid>

          <div className="mt-6 rounded-lg border border-parchment-line bg-white/60 p-6">
            <PanelHeader icon={Settings2} title="Manage College" />

            <div className="flex flex-wrap items-center gap-4">
              <button
                type="button"
                onClick={toggleStatus}
                disabled={statusBusy}
                className={`flex items-center gap-1.5 text-sm font-medium disabled:opacity-60 ${
                  details.college.isActive ? 'text-brick hover:text-brick' : 'text-green-700 hover:text-green-800'
                }`}
              >
                {statusBusy ? (
                  <Loader2 size={14} className="animate-spin" />
                ) : details.college.isActive ? (
                  <Ban size={14} />
                ) : (
                  <PlayCircle size={14} />
                )}
                {details.college.isActive ? 'Suspend' : 'Reactivate'}
              </button>

              <button
                type="button"
                onClick={() => setEditingSubscription((v) => !v)}
                className="flex items-center gap-1.5 text-sm text-slate hover:text-ink"
              >
                <Pencil size={14} />
                Edit Subscription
              </button>

              <button
                type="button"
                onClick={() => setConfirmingDelete(true)}
                className="ml-auto flex items-center gap-1.5 text-sm text-brick hover:text-brick"
              >
                <Trash2 size={14} />
                Delete
              </button>
            </div>

            {statusError && <p className="mt-2 text-xs text-brick">{statusError}</p>}

            {editingSubscription && (
              <form onSubmit={saveSubscription} className="mt-4 max-w-md space-y-3 rounded-md bg-parchment/60 p-4">
                <div className="grid grid-cols-2 gap-3">
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
              <div className="mt-4 max-w-md space-y-2 rounded-md border border-brick/40 bg-brick/5 p-4">
                <p className="flex items-center gap-1.5 text-xs font-medium text-brick">
                  <AlertTriangle size={13} />
                  This permanently deletes "{details.college.collegeName}" and every student, teacher, and
                  record it has. This cannot be undone.
                </p>
                <p className="text-xs text-slate-dim">
                  Type <span className="font-mono text-ink">{details.college.subdomain}</span> to confirm.
                </p>
                <input
                  value={confirmText}
                  onChange={(e) => setConfirmText(e.target.value)}
                  className={`${inputClass} font-mono`}
                  placeholder={details.college.subdomain}
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
                    disabled={confirmText.trim() !== details.college.subdomain || deleteBusy}
                    onClick={confirmDelete}
                    className="flex items-center gap-1 rounded-md bg-brick px-3 py-1.5 text-xs font-medium text-white disabled:opacity-40"
                  >
                    {deleteBusy && <Loader2 size={12} className="animate-spin" />}
                    Permanently delete
                  </button>
                </div>
              </div>
            )}
          </div>
        </>
      )}
    </Layout>
  )
}

function StatTile({ icon, label, value }: { icon: ReactNode; label: string; value: number }) {
  return (
    <div className="paper paper-interactive rounded-md border border-parchment-line/70 bg-white/60 p-3 text-center">
      <div className="flex items-center justify-center gap-1 text-slate-dim">{icon}</div>
      <p className="mt-1 font-display text-xl font-medium text-ink">{value}</p>
      <p className="text-[10px] uppercase tracking-wide text-slate-dim">{label}</p>
    </div>
  )
}