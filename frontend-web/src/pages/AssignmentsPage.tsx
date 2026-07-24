// src/pages/AssignmentsPage.tsx
import { useEffect, useMemo, useState } from 'react'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
import { useAuthStore } from '@/store/authStore'
import { StampGrid } from '@/components/motion'
import { StatCard, PanelError, STAT_SHADES } from '@/components/PageBits'
import {
  ClipboardList,
  CalendarClock,
  AlertTriangle,
  FileCheck2,
  Search,
  Filter,
  Plus,
  Pencil,
  Trash2,
  X,
  Award,
} from 'lucide-react'
import {
  getAssignments,
  createAssignment,
  updateAssignment,
  deleteAssignment,
  getSubmissions,
  type Assignment,
  type AssignmentPayload,
  type AssignmentSubmission,
} from '@/api/assignment'
import { getSubjects, type Subject } from '@/api/subject'

function todayISO() {
  return new Date().toISOString().slice(0, 10)
}

function daysUntil(dueDate: string) {
  const due = new Date(dueDate.slice(0, 10))
  const today = new Date(todayISO())
  return Math.round((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
}

type DueFilter = '' | 'upcoming' | 'overdue'

const emptyForm = {
  subjectId: '',
  teacherId: '',
  title: '',
  description: '',
  dueDate: '',
  maxMarks: '',
}

export function AssignmentsPage() {
  const { user } = useAuthStore()
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [subjects, setSubjects] = useState<Subject[]>([])
  const [submissions, setSubmissions] = useState<AssignmentSubmission[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [search, setSearch] = useState('')
  const [subjectFilter, setSubjectFilter] = useState('')
  const [dueFilter, setDueFilter] = useState<DueFilter>('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Assignment | null>(null)
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function loadAll() {
    setLoading(true)
    setError(null)
    try {
      const [a, s, sub] = await Promise.all([getAssignments(), getSubjects(), getSubmissions()])
      setAssignments(a)
      setSubjects(s)
      setSubmissions(sub)
    } catch {
      setError('Failed to load assignments. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAll()
  }, [])

  const subjectNameById = useMemo(() => {
    const m = new Map<number, string>()
    subjects.forEach((s) => m.set(s.id, s.subjectName))
    return m
  }, [subjects])

  const stats = useMemo(() => {
    const total = assignments.length
    const dueThisWeek = assignments.filter((a) => {
      const d = daysUntil(a.dueDate)
      return d >= 0 && d <= 7
    }).length
    const overdue = assignments.filter((a) => daysUntil(a.dueDate) < 0).length
    const pendingReview = submissions.filter((s) => s.status !== 'EVALUATED').length
    return { total, dueThisWeek, overdue, pendingReview }
  }, [assignments, submissions])

  const filtered = useMemo(() => {
    return assignments.filter((a) => {
      const matchesSearch =
        !search ||
        a.title.toLowerCase().includes(search.toLowerCase()) ||
        a.subjectName.toLowerCase().includes(search.toLowerCase())
      const matchesSubject = !subjectFilter || String(a.subjectId) === subjectFilter
      const matchesDue =
        !dueFilter ||
        (dueFilter === 'overdue' && daysUntil(a.dueDate) < 0) ||
        (dueFilter === 'upcoming' && daysUntil(a.dueDate) >= 0)
      return matchesSearch && matchesSubject && matchesDue
    })
  }, [assignments, search, subjectFilter, dueFilter])

  const hasFilters = !!search || !!subjectFilter || !!dueFilter

  function openCreateModal() {
    setEditing(null)
    setForm({ ...emptyForm, teacherId: user?.id ? String(user.id) : '' })
    setFormError(null)
    setModalOpen(true)
  }

  function openEditModal(a: Assignment) {
    setEditing(a)
    setForm({
      subjectId: String(a.subjectId),
      teacherId: String(a.teacherId),
      title: a.title,
      description: a.description,
      dueDate: a.dueDate.slice(0, 10),
      maxMarks: String(a.maxMarks),
    })
    setFormError(null)
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setEditing(null)
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.subjectId || !form.teacherId || !form.title || !form.dueDate || !form.maxMarks) {
      setFormError('Please fill in all required fields.')
      return
    }
    setSaving(true)
    setFormError(null)
    const payload: AssignmentPayload = {
      subjectId: Number(form.subjectId),
      teacherId: Number(form.teacherId),
      title: form.title,
      description: form.description,
      dueDate: form.dueDate,
      maxMarks: Number(form.maxMarks),
    }
    try {
      if (editing) {
        await updateAssignment(editing.id, payload)
      } else {
        await createAssignment(payload)
      }
      await loadAll()
      closeModal()
    } catch {
      setFormError('Failed to save the assignment. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: number) {
    if (!window.confirm('Delete this assignment? This cannot be undone.')) return
    try {
      await deleteAssignment(id)
      setAssignments((prev) => prev.filter((a) => a.id !== id))
    } catch {
      window.alert('Failed to delete the assignment. Please try again.')
    }
  }

  return (
    <Layout>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-display text-2xl font-medium text-ink">Assignments</h1>
          <p className="mt-1 text-sm text-slate-dim">Create and manage assignments across subjects</p>
        </div>
        <button
          onClick={openCreateModal}
          className="inline-flex items-center gap-2 rounded-lg bg-brass px-4 py-2 text-sm font-medium text-white hover:bg-brass/90"
        >
          <Plus size={16} />
          Create Assignment
        </button>
      </div>

      {/* Dashboard cards */}
      <StampGrid className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard
          icon={ClipboardList}
          label="Total Assignments"
          value={stats.total}
          accent={STAT_SHADES[0]}
          failed={!!error}
        />
        <StatCard
          icon={CalendarClock}
          label="Due This Week"
          value={stats.dueThisWeek}
          accent={STAT_SHADES[1]}
          failed={!!error}
        />
        <StatCard
          icon={AlertTriangle}
          label="Overdue"
          value={stats.overdue}
          accent={STAT_SHADES[2]}
          failed={!!error}
        />
        <StatCard
          icon={FileCheck2}
          label="Pending Review"
          value={stats.pendingReview}
          accent={STAT_SHADES[3]}
          failed={!!error}
        />
      </StampGrid>

      {/* Search + filters */}
      <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-dim" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by title or subject..."
            className="w-full rounded-lg border border-parchment-line bg-white/60 py-2 pl-9 pr-3 text-sm text-ink placeholder:text-slate-dim focus:border-brass focus:outline-none"
          />
        </div>

        <div className="flex items-center gap-2">
          <Filter size={16} className="text-slate-dim" />
          <select
            value={subjectFilter}
            onChange={(e) => setSubjectFilter(e.target.value)}
            className="rounded-lg border border-parchment-line bg-white/60 px-3 py-2 text-sm text-ink focus:border-brass focus:outline-none"
          >
            <option value="">All Subjects</option>
            {subjects.map((s) => (
              <option key={s.id} value={s.id}>
                {s.subjectName}
              </option>
            ))}
          </select>

          <select
            value={dueFilter}
            onChange={(e) => setDueFilter(e.target.value as DueFilter)}
            className="rounded-lg border border-parchment-line bg-white/60 px-3 py-2 text-sm text-ink focus:border-brass focus:outline-none"
          >
            <option value="">All Due Dates</option>
            <option value="upcoming">Upcoming</option>
            <option value="overdue">Overdue</option>
          </select>

          {hasFilters && (
            <button
              onClick={() => {
                setSearch('')
                setSubjectFilter('')
                setDueFilter('')
              }}
              className="inline-flex items-center gap-1 rounded-lg border border-parchment-line px-3 py-2 text-sm text-slate-dim hover:border-brass hover:text-ink"
            >
              <X size={14} />
              Clear
            </button>
          )}
        </div>
      </div>

      {/* Table */}
      <div className="mt-6 overflow-hidden rounded-lg border border-parchment-line bg-white/60">
        {loading ? (
          <div className="p-10 text-center text-sm text-slate-dim">Loading assignments...</div>
        ) : error ? (
          <div className="p-10">
            <PanelError message={error} />
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-2 p-12 text-center">
            <ClipboardList size={32} className="text-slate-dim/50" />
            <p className="font-display text-base font-medium text-ink">
              {hasFilters ? 'No assignments match your filters' : 'No assignments yet'}
            </p>
            <p className="text-sm text-slate-dim">
              {hasFilters ? 'Try adjusting your search or filters.' : 'Create an assignment to get started.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-200 text-left text-sm">
              <thead>
                <tr className="border-b border-parchment-line text-slate-dim">
                  <th className="px-5 py-3 font-medium">Title</th>
                  <th className="px-5 py-3 font-medium">Subject</th>
                  <th className="px-5 py-3 font-medium">Teacher</th>
                  <th className="px-5 py-3 font-medium">Due Date</th>
                  <th className="px-5 py-3 font-medium">Max Marks</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                  <th className="px-5 py-3 font-medium text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((a) => {
                  const overdue = daysUntil(a.dueDate) < 0
                  return (
                    <tr key={a.id} className="border-b border-parchment-line last:border-0 hover:bg-white/80">
                      <td className="px-5 py-3 text-ink">{a.title}</td>
                      <td className="px-5 py-3 text-slate-dim">{subjectNameById.get(a.subjectId) || a.subjectName}</td>
                      <td className="px-5 py-3 text-slate-dim">{a.teacherName}</td>
                      <td className="px-5 py-3 text-slate-dim">{a.dueDate.slice(0, 10)}</td>
                      <td className="px-5 py-3 text-slate-dim">{a.maxMarks}</td>
                      <td className="px-5 py-3">
                        <span
                          className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                            overdue ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'
                          }`}
                        >
                          {overdue ? 'Overdue' : 'Upcoming'}
                        </span>
                      </td>
                      <td className="px-5 py-3">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => openEditModal(a)}
                            className="rounded-md p-1.5 text-slate-dim hover:bg-parchment-line/50 hover:text-brass"
                            aria-label="Edit assignment"
                          >
                            <Pencil size={16} />
                          </button>
                          <button
                            onClick={() => handleDelete(a.id)}
                            className="rounded-md p-1.5 text-slate-dim hover:bg-red-50 hover:text-red-600"
                            aria-label="Delete assignment"
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create/Edit modal — conditionally rendered, Modal has no isOpen prop */}
      {modalOpen && (
        <Modal onClose={closeModal} title={editing ? 'Edit Assignment' : 'Create Assignment'}>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Subject">
                <select
                  value={form.subjectId}
                  onChange={(e) => setForm((f) => ({ ...f, subjectId: e.target.value }))}
                  className={inputClass}
                >
                  <option value="">Select a subject</option>
                  {subjects.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.subjectName} · {s.courseName}
                    </option>
                  ))}
                </select>
              </Field>

              <Field label="Teacher ID">
                <input
                  type="number"
                  value={form.teacherId}
                  onChange={(e) => setForm((f) => ({ ...f, teacherId: e.target.value }))}
                  className={inputClass}
                  placeholder="Teacher ID"
                />
              </Field>
            </div>

            <Field label="Title">
              <input
                value={form.title}
                onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                className={inputClass}
                placeholder="Assignment title"
              />
            </Field>

            <Field label="Description">
              <textarea
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                className={`${inputClass} min-h-24 resize-y`}
                placeholder="Assignment details and instructions"
              />
            </Field>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Due Date">
                <input
                  type="date"
                  value={form.dueDate}
                  onChange={(e) => setForm((f) => ({ ...f, dueDate: e.target.value }))}
                  className={inputClass}
                />
              </Field>

              <Field label="Max Marks">
                <input
                  type="number"
                  min={0}
                  value={form.maxMarks}
                  onChange={(e) => setForm((f) => ({ ...f, maxMarks: e.target.value }))}
                  className={inputClass}
                  placeholder="100"
                />
              </Field>
            </div>

            {formError && <p className="text-sm text-red-600">{formError}</p>}

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={closeModal}
                className="rounded-lg border border-parchment-line px-4 py-2 text-sm text-slate-dim hover:border-brass hover:text-ink"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={saving}
                className="inline-flex items-center gap-2 rounded-lg bg-brass px-4 py-2 text-sm font-medium text-white hover:bg-brass/90 disabled:opacity-60"
              >
                <Award size={16} />
                {saving ? 'Saving...' : editing ? 'Save Changes' : 'Create Assignment'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </Layout>
  )
}