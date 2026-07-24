// src/pages/CoursesPage.tsx
import { useEffect, useState, type FormEvent } from 'react'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
import { StampGrid } from '@/components/motion'
import { StatCard, PanelError, STAT_SHADES } from '@/components/PageBits'
import {
    getCourses,
    createCourse,
    updateCourse,
    deleteCourse,
    getDepartments,
    type Course,
    type CoursePayload,
    type Department,
} from '@/api'
import { Plus, Pencil, Trash2, Loader2, BookOpen, Search } from 'lucide-react'

const emptyForm = {
  courseCode: '',
  courseName: '',
  duration: undefined as number | undefined,
  description: '',
  departmentId: '' as number | '',
}
type FormState = typeof emptyForm

export function CoursesPage() {
  const [courses, setCourses] = useState<Course[]>([])
  const [departments, setDepartments] = useState<Department[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Course | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [c, d] = await Promise.all([getCourses(), getDepartments()])
      setCourses(c)
      setDepartments(d)
    } catch {
      setError('Could not load courses.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const filteredCourses = courses.filter((c) => {
    const q = search.toLowerCase()
    return (
      c.courseCode.toLowerCase().includes(q) ||
      c.courseName.toLowerCase().includes(q) ||
      (c.departmentName ?? '').toLowerCase().includes(q)
    )
  })

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setFormError(null)
    setModalOpen(true)
  }

  function openEdit(course: Course) {
    setEditing(course)
    setForm({
      courseCode: course.courseCode,
      courseName: course.courseName,
      duration: course.duration,
      description: course.description ?? '',
      departmentId: course.departmentId ?? '',
    })
    setFormError(null)
    setModalOpen(true)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!form.departmentId) {
      setFormError('Please select a department.')
      return
    }
    if (!form.duration) {
      setFormError('Please enter the course duration.')
      return
    }
    setSaving(true)
    setFormError(null)
    const payload: CoursePayload = {
      courseCode: form.courseCode,
      courseName: form.courseName,
      duration: form.duration,
      description: form.description,
      departmentId: Number(form.departmentId),
    }
    try {
      if (editing) await updateCourse(editing.id, payload)
      else await createCourse(payload)
      setModalOpen(false)
      await load()
    } catch {
      setFormError('Could not save this course. Check the fields and try again.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('Delete this course? This cannot be undone.')) return
    try {
      await deleteCourse(id)
      await load()
    } catch {
      alert('Could not delete this course.')
    }
  }

  return (
    <Layout>
      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-display text-2xl font-medium text-ink">Courses</h1>
          <p className="mt-1 text-sm text-slate-dim">Programs offered within each department.</p>
        </div>
        <button
          onClick={openCreate}
          className="flex items-center justify-center gap-2 rounded-md bg-brass px-4 py-2 text-sm font-medium text-white hover:bg-brass-bright"
        >
          <Plus size={16} /> Add course
        </button>
      </div>

      <StampGrid className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
        <StatCard icon={BookOpen} label="Total Courses" value={courses.length} accent={STAT_SHADES[0]} />
        <StatCard icon={Search} label="Showing" value={filteredCourses.length} accent={STAT_SHADES[3]} />
      </StampGrid>

      <div className="relative mb-6">
        <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-dim" />
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by code, name or department..."
          className="w-full rounded-lg border border-parchment-line bg-white/60 py-2 pl-9 pr-3 text-sm text-ink placeholder:text-slate-dim focus:border-brass focus:outline-none sm:max-w-md"
        />
      </div>

      {loading ? (
        <p className="text-sm text-slate-dim">Loading…</p>
      ) : error ? (
        <PanelError message={error} />
      ) : filteredCourses.length === 0 ? (
        <p className="text-sm text-slate-dim">{search ? 'No courses match your search.' : 'No courses yet.'}</p>
      ) : (
        <>
          <div className="hidden overflow-hidden rounded-lg border border-parchment-line bg-white/50 sm:block">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-parchment-line text-xs uppercase tracking-wide text-slate-dim">
                  <th className="px-4 py-3 font-medium">Code</th>
                  <th className="px-4 py-3 font-medium">Name</th>
                  <th className="px-4 py-3 font-medium">Department</th>
                  <th className="px-4 py-3 font-medium">Duration</th>
                  <th className="px-4 py-3 font-medium"></th>
                </tr>
              </thead>
              <tbody>
                {filteredCourses.map((c) => (
                  <tr key={c.id} className="border-b border-parchment-line last:border-0">
                    <td className="px-4 py-3 font-mono text-xs text-ink">{c.courseCode}</td>
                    <td className="px-4 py-3 text-ink">{c.courseName}</td>
                    <td className="px-4 py-3 text-slate-dim">{c.departmentName}</td>
                    <td className="px-4 py-3 text-slate-dim">{c.duration ? `${c.duration} yrs` : '—'}</td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-3">
                        <button onClick={() => openEdit(c)} className="text-slate hover:text-ink">
                          <Pencil size={15} />
                        </button>
                        <button onClick={() => handleDelete(c.id)} className="text-slate hover:text-brick">
                          <Trash2 size={15} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="space-y-3 sm:hidden">
            {filteredCourses.map((c) => (
              <li key={c.id} className="rounded-lg border border-parchment-line bg-white/60 p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-display text-sm font-medium text-ink">{c.courseName}</p>
                    <p className="font-mono text-xs text-slate-dim">{c.courseCode}</p>
                  </div>
                  <div className="flex shrink-0 gap-3">
                    <button onClick={() => openEdit(c)} className="text-slate hover:text-ink">
                      <Pencil size={15} />
                    </button>
                    <button onClick={() => handleDelete(c.id)} className="text-slate hover:text-brick">
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>
                <p className="mt-2 text-xs text-slate-dim">{c.departmentName}</p>
                <p className="mt-1 text-xs text-slate-dim">{c.duration ? `${c.duration} yrs` : '—'}</p>
              </li>
            ))}
          </ul>
        </>
      )}

      {modalOpen && (
        <Modal title={editing ? 'Edit course' : 'Add course'} onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit} className="space-y-4">
            <Field label="Course code">
              <input required value={form.courseCode} onChange={(e) => setForm({ ...form, courseCode: e.target.value })} className={inputClass} />
            </Field>
            <Field label="Course name">
              <input required value={form.courseName} onChange={(e) => setForm({ ...form, courseName: e.target.value })} className={inputClass} />
            </Field>
            <Field label="Department">
              <select required value={form.departmentId} onChange={(e) => setForm({ ...form, departmentId: Number(e.target.value) })} className={inputClass}>
                <option value="">Select department</option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>{d.name}</option>
                ))}
              </select>
            </Field>
            <Field label="Duration (years)">
              <input
                type="number"
                min={1}
                value={form.duration ?? ''}
                onChange={(e) => setForm({ ...form, duration: e.target.value ? Number(e.target.value) : undefined })}
                className={inputClass}
              />
            </Field>
            <Field label="Description">
              <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} rows={3} className={inputClass} />
            </Field>
            {formError && <p className="text-sm text-brick">{formError}</p>}
            <div className="flex flex-col-reverse justify-end gap-3 pt-2 sm:flex-row">
              <button type="button" onClick={() => setModalOpen(false)} className="rounded-md border border-parchment-line px-4 py-2 text-sm text-slate-dim hover:text-ink">
                Cancel
              </button>
              <button type="submit" disabled={saving} className="flex items-center justify-center gap-2 rounded-md bg-brass px-4 py-2 text-sm font-medium text-white hover:bg-brass-bright disabled:opacity-60">
                {saving && <Loader2 size={14} className="animate-spin" />}
                Save
              </button>
            </div>
          </form>
        </Modal>
      )}
    </Layout>
  )
}