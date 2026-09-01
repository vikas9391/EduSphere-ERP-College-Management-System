import { useEffect, useMemo, useState } from 'react'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
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
import { getMyTeachingClassSubjects, type ClassSubject } from '@/api/schoolClass'

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
  classSubjectId: '',
  subjectId: '',
  teacherId: '',
  title: '',
  description: '',
  dueDate: '',
  maxMarks: '',
}

export function AssignmentsPage() {
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [classSubjects, setClassSubjects] = useState<ClassSubject[]>([])
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
      const [a, offerings, sub] = await Promise.all([
        getAssignments(),
        getMyTeachingClassSubjects(),
        getSubmissions(),
      ])
      setAssignments(a)
      setClassSubjects(offerings)
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

  const usableClassSubjects = useMemo(
    () => classSubjects.filter((cs) => cs.linkedSubjectId != null),
    [classSubjects],
  )

  const subjectOptions = useMemo(() => {
    const m = new Map<number, string>()
    assignments.forEach((a) => m.set(a.subjectId, a.subjectName))
    usableClassSubjects.forEach((cs) => {
      if (cs.linkedSubjectId != null) {
        m.set(cs.linkedSubjectId, cs.linkedSubjectName || cs.subjectName)
      }
    })
    return Array.from(m.entries()).sort((a, b) => a[1].localeCompare(b[1]))
  }, [assignments, usableClassSubjects])

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
        a.subjectName.toLowerCase().includes(search.toLowerCase()) ||
        (a.className || '').toLowerCase().includes(search.toLowerCase())
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
    setForm(emptyForm)
    setFormError(null)
    setModalOpen(true)
  }

  function openEditModal(a: Assignment) {
    setEditing(a)
    setForm({
      classSubjectId: a.classSubjectId ? String(a.classSubjectId) : '',
      subjectId: String(a.subjectId),
      teacherId: String(a.teacherId),
      title: a.title,
      description: a.description || '',
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

  function selectClassSubject(value: string) {
    const selected = usableClassSubjects.find((cs) => String(cs.id) === value)
    setForm((current) => ({
      ...current,
      classSubjectId: value,
      subjectId: selected?.linkedSubjectId ? String(selected.linkedSubjectId) : '',
      teacherId: selected ? String(selected.teacherId) : '',
    }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (
      !form.classSubjectId ||
      !form.subjectId ||
      !form.teacherId ||
      !form.title.trim() ||
      !form.dueDate ||
      !form.maxMarks
    ) {
      setFormError('Select a class subject and fill in all required fields.')
      return
    }

    const maxMarks = Number(form.maxMarks)
    if (!Number.isFinite(maxMarks) || maxMarks <= 0) {
      setFormError('Maximum marks must be greater than zero.')
      return
    }

    setSaving(true)
    setFormError(null)
    const payload: AssignmentPayload = {
      classSubjectId: Number(form.classSubjectId),
      subjectId: Number(form.subjectId),
      teacherId: Number(form.teacherId),
      title: form.title.trim(),
      description: form.description,
      dueDate: form.dueDate,
      maxMarks,
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
      setFormError('Failed to save the assignment. Check the selected class subject and try again.')
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
          <h1 className="font-heading text-2xl font-medium text-text">Assignments</h1>
          <p className="mt-1 text-sm text-muted">Create assignments for an exact class subject and roster</p>
        </div>
        <button
          onClick={openCreateModal}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary/90"
        >
          <Plus size={16} />
          Create Assignment
        </button>
      </div>

      <StampGrid className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard icon={ClipboardList} label="Total Assignments" value={stats.total} accent={STAT_SHADES[0]} failed={!!error} />
        <StatCard icon={CalendarClock} label="Due This Week" value={stats.dueThisWeek} accent={STAT_SHADES[1]} failed={!!error} />
        <StatCard icon={AlertTriangle} label="Overdue" value={stats.overdue} accent={STAT_SHADES[2]} failed={!!error} />
        <StatCard icon={FileCheck2} label="Pending Review" value={stats.pendingReview} accent={STAT_SHADES[3]} failed={!!error} />
      </StampGrid>

      <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by title, subject, or class..."
            className="w-full rounded-lg border border-border bg-white/60 py-2 pl-9 pr-3 text-sm text-text placeholder:text-muted focus:border-primary focus:outline-none"
          />
        </div>

        <div className="flex items-center gap-2">
          <Filter size={16} className="text-muted" />
          <select
            value={subjectFilter}
            onChange={(e) => setSubjectFilter(e.target.value)}
            className="rounded-lg border border-border bg-white/60 px-3 py-2 text-sm text-text focus:border-primary focus:outline-none"
          >
            <option value="">All Subjects</option>
            {subjectOptions.map(([id, name]) => (
              <option key={id} value={id}>{name}</option>
            ))}
          </select>

          <select
            value={dueFilter}
            onChange={(e) => setDueFilter(e.target.value as DueFilter)}
            className="rounded-lg border border-border bg-white/60 px-3 py-2 text-sm text-text focus:border-primary focus:outline-none"
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
              className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-2 text-sm text-muted hover:border-primary hover:text-text"
            >
              <X size={14} />
              Clear
            </button>
          )}
        </div>
      </div>

      <div className="mt-6 overflow-hidden rounded-lg border border-border bg-white/60">
        {loading ? (
          <div className="p-10 text-center text-sm text-muted">Loading assignments...</div>
        ) : error ? (
          <div className="p-10"><PanelError message={error} /></div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-2 p-12 text-center">
            <ClipboardList size={32} className="text-muted/50" />
            <p className="font-heading text-base font-medium text-text">
              {hasFilters ? 'No assignments match your filters' : 'No assignments yet'}
            </p>
            <p className="text-sm text-muted">
              {hasFilters ? 'Try adjusting your search or filters.' : 'Create an assignment to get started.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-200 text-left text-sm">
              <thead>
                <tr className="border-b border-border text-muted">
                  <th className="px-5 py-3 font-medium">Title</th>
                  <th className="px-5 py-3 font-medium">Class / Subject</th>
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
                    <tr key={a.id} className="border-b border-border last:border-0 hover:bg-white/80">
                      <td className="px-5 py-3 text-text">{a.title}</td>
                      <td className="px-5 py-3 text-muted">
                        <div className="text-text">{a.subjectName}</div>
                        <div className="text-xs text-muted">{a.className || 'Legacy subject-only assignment'}</div>
                      </td>
                      <td className="px-5 py-3 text-muted">{a.teacherName}</td>
                      <td className="px-5 py-3 text-muted">{a.dueDate.slice(0, 10)}</td>
                      <td className="px-5 py-3 text-muted">{a.maxMarks}</td>
                      <td className="px-5 py-3">
                        <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${overdue ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>
                          {overdue ? 'Overdue' : 'Upcoming'}
                        </span>
                      </td>
                      <td className="px-5 py-3">
                        <div className="flex items-center justify-end gap-2">
                          <button onClick={() => openEditModal(a)} className="rounded-md p-1.5 text-muted hover:bg-border/50 hover:text-primary" aria-label="Edit assignment">
                            <Pencil size={16} />
                          </button>
                          <button onClick={() => handleDelete(a.id)} className="rounded-md p-1.5 text-muted hover:bg-red-50 hover:text-red-600" aria-label="Delete assignment">
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

      {modalOpen && (
        <Modal onClose={closeModal} title={editing ? 'Edit Assignment' : 'Create Assignment'}>
          <form onSubmit={handleSubmit} className="space-y-4">
            <Field label="Class Subject">
              <select
                value={form.classSubjectId}
                onChange={(e) => selectClassSubject(e.target.value)}
                className={inputClass}
              >
                <option value="">Select the exact class subject</option>
                {usableClassSubjects.map((cs) => (
                  <option key={cs.id} value={cs.id}>
                    {cs.schoolClassName || `Class ${cs.schoolClassId}`} · {cs.subjectName} · {cs.academicYear || 'Year n/a'} · Sem {cs.semester ?? '-'} · {cs.teacherName}
                  </option>
                ))}
              </select>
            </Field>

            {editing && !editing.classSubjectId && (
              <p className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
                This is a legacy subject-only assignment. Select its exact class subject before saving so it is scoped to one roster.
              </p>
            )}

            {usableClassSubjects.length === 0 && (
              <p className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
                No class subjects are linked to formal curriculum subjects yet. Link a formal subject from the class setup page before creating assignments.
              </p>
            )}

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Formal Subject">
                <input
                  value={usableClassSubjects.find((cs) => String(cs.id) === form.classSubjectId)?.linkedSubjectName || ''}
                  readOnly
                  className={inputClass}
                  placeholder="Selected automatically"
                />
              </Field>
              <Field label="Teacher">
                <input
                  value={usableClassSubjects.find((cs) => String(cs.id) === form.classSubjectId)?.teacherName || ''}
                  readOnly
                  className={inputClass}
                  placeholder="Selected automatically"
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
                  min={1}
                  value={form.maxMarks}
                  onChange={(e) => setForm((f) => ({ ...f, maxMarks: e.target.value }))}
                  className={inputClass}
                  placeholder="100"
                />
              </Field>
            </div>

            {formError && <p className="text-sm text-red-600">{formError}</p>}

            <div className="flex justify-end gap-2 pt-2">
              <button type="button" onClick={closeModal} className="rounded-lg border border-border px-4 py-2 text-sm text-muted hover:border-primary hover:text-text">
                Cancel
              </button>
              <button
                type="submit"
                disabled={saving || usableClassSubjects.length === 0}
                className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary/90 disabled:opacity-60"
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
