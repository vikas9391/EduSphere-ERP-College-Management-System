// src/pages/SubjectsPage.tsx
import { useEffect, useState, type FormEvent } from 'react'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
import { StampGrid } from '@/components/motion'
import {
  getSubjects,
  createSubject,
  updateSubject,
  deleteSubject,
  getCourses,
  getTeachers,
  type Subject,
  type SubjectPayload,
  type Course,
  type Teacher,
} from '@/api'
import { Plus, Pencil, Trash2, Loader2, Search, Layers } from 'lucide-react'
import { StatCard, PanelError, Badge, STAT_SHADES } from '@/components/PageBits'

const emptyForm = {
  subjectCode: '',
  subjectName: '',
  credits: 1,
  semester: 1,
  courseId: '' as number | '',
  teacherId: '' as number | '',
}
type FormState = typeof emptyForm

export function SubjectsPage() {
  const [subjects, setSubjects] = useState<Subject[]>([])
  const [courses, setCourses] = useState<Course[]>([])
  const [teachers, setTeachers] = useState<Teacher[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Subject | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [s, c, t] = await Promise.all([getSubjects(), getCourses(), getTeachers()])
      setSubjects(s)
      setCourses(c)
      setTeachers(t)
    } catch {
      setError('Could not load subjects.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const filteredSubjects = subjects.filter((subject) => {
    const value = search.toLowerCase()
    return (
      subject.subjectCode.toLowerCase().includes(value) ||
      subject.subjectName.toLowerCase().includes(value) ||
      subject.courseName.toLowerCase().includes(value) ||
      subject.teacherName.toLowerCase().includes(value)
    )
  })

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setFormError(null)
    setModalOpen(true)
  }

  function openEdit(subject: Subject) {
    setEditing(subject)
    setForm({
      subjectCode: subject.subjectCode,
      subjectName: subject.subjectName,
      credits: subject.credits,
      semester: subject.semester,
      courseId: subject.courseId,
      teacherId: subject.teacherId,
    })
    setFormError(null)
    setModalOpen(true)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!form.courseId || !form.teacherId) {
      setFormError('Please select both a course and a teacher.')
      return
    }
    setSaving(true)
    setFormError(null)
    const payload: SubjectPayload = {
      subjectCode: form.subjectCode,
      subjectName: form.subjectName,
      credits: form.credits,
      semester: form.semester,
      courseId: Number(form.courseId),
      teacherId: Number(form.teacherId),
    }
    try {
      if (editing) await updateSubject(editing.id, payload)
      else await createSubject(payload)
      setModalOpen(false)
      await load()
    } catch {
      setFormError('Could not save this subject. Check the fields and try again.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('Delete this subject? This cannot be undone.')) return
    try {
      await deleteSubject(id)
      await load()
    } catch {
      alert('Could not delete this subject.')
    }
  }

  return (
    <Layout>
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-display text-2xl font-medium text-ink">Subjects</h1>
          <p className="mt-1 text-sm text-slate-dim">Subjects mapped to courses and teachers.</p>
        </div>
      </div>

      <StampGrid className="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard icon={Layers} label="Total Subjects" value={subjects.length} accent={STAT_SHADES[0]} />
        <StatCard icon={Layers} label="Courses" value={courses.length} accent={STAT_SHADES[2]} />
        <StatCard icon={Layers} label="Teachers" value={teachers.length} accent={STAT_SHADES[4]} />
        <StatCard icon={Layers} label="Semesters" value={8} accent={STAT_SHADES[6]} />
      </StampGrid>

      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative w-full sm:max-w-md">
          <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-dim" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search subjects..."
            className="w-full rounded-md border border-parchment-line bg-white/60 py-2 pl-9 pr-3 text-sm text-ink placeholder:text-slate-dim focus:border-brass focus:outline-none"
          />
        </div>

        <button
          onClick={openCreate}
          className="flex items-center justify-center gap-2 rounded-md bg-brass px-4 py-2 text-sm font-medium text-white hover:bg-brass-bright"
        >
          <Plus size={16} />
          Add Subject
        </button>
      </div>

      {loading ? (
        <p className="text-sm text-slate-dim">Loading…</p>
      ) : error ? (
        <PanelError message={error} />
      ) : filteredSubjects.length === 0 ? (
        <div className="rounded-lg border border-dashed border-parchment-line bg-white/60 p-10 text-center">
          <h3 className="font-display text-xl text-ink">No Subjects Found</h3>
          <p className="mt-2 text-slate-dim">Create your first subject.</p>
          <button onClick={openCreate} className="mt-5 rounded-md bg-brass px-5 py-2 text-sm font-medium text-white hover:bg-brass-bright">
            Add Subject
          </button>
        </div>
      ) : (
        <>
          <div className="hidden overflow-hidden rounded-lg border border-parchment-line bg-white/50 sm:block">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-parchment-line text-xs uppercase tracking-wide text-slate-dim">
                  <th className="px-4 py-3 font-medium">Subject</th>
                  <th className="px-4 py-3 font-medium">Course</th>
                  <th className="px-4 py-3 font-medium">Faculty</th>
                  <th className="px-4 py-3 font-medium">Semester</th>
                  <th className="px-4 py-3 font-medium">Credits</th>
                  <th className="px-4 py-3 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredSubjects.map((s) => (
                  <tr key={s.id} className="border-b border-parchment-line last:border-0">
                    <td className="px-4 py-3 text-ink">
                      <div>
                        <p className="font-medium">{s.subjectName}</p>
                        <p className="text-xs text-slate-dim">{s.subjectCode}</p>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-slate-dim">{s.courseName}</td>
                    <td className="px-4 py-3 text-slate-dim">{s.teacherName}</td>
                    <td className="px-4 py-3">
  <Badge variant="neutral">Semester {s.semester}</Badge>
</td>
<td className="px-4 py-3">
  <Badge variant="success">{s.credits} Credits</Badge>
</td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-2">
                        <button title="Edit" onClick={() => openEdit(s)} className="rounded p-2 hover:bg-brass/10">
                          <Pencil size={16} />
                        </button>
                        <button title="Delete" onClick={() => handleDelete(s.id)} className="rounded p-2 hover:bg-brick/10">
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="space-y-3 sm:hidden">
            {filteredSubjects.map((s) => (
              <li key={s.id} className="rounded-lg border border-parchment-line bg-white/60 p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-display text-sm font-medium text-ink">{s.subjectName}</p>
                    <p className="text-xs text-slate-dim">{s.subjectCode}</p>
                  </div>
                  <div className="flex shrink-0 gap-2">
                    <button title="Edit" onClick={() => openEdit(s)} className="rounded p-1.5 hover:bg-brass/10">
                      <Pencil size={15} />
                    </button>
                    <button title="Delete" onClick={() => handleDelete(s.id)} className="rounded p-1.5 hover:bg-brick/10">
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>
                <p className="mt-2 text-xs text-slate-dim">{s.courseName} · {s.teacherName}</p>
                <div className="mt-2 flex gap-2">
                  <span className="rounded bg-blue-100 px-2 py-0.5 text-xs text-blue-700">Sem {s.semester}</span>
                  <span className="rounded bg-green-100 px-2 py-0.5 text-xs text-green-700">{s.credits} Credits</span>
                </div>
              </li>
            ))}
          </ul>
        </>
      )}

      {modalOpen && (
        <Modal title={editing ? 'Update Subject' : 'Create Subject'} onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Subject code">
                <input required value={form.subjectCode} onChange={(e) => setForm({ ...form, subjectCode: e.target.value })} className={inputClass} />
              </Field>
              <Field label="Subject name">
                <input required value={form.subjectName} onChange={(e) => setForm({ ...form, subjectName: e.target.value })} className={inputClass} />
              </Field>
              <Field label="Course">
                <select required value={form.courseId} onChange={(e) => setForm({ ...form, courseId: Number(e.target.value) })} className={inputClass}>
                  <option value="">Select course</option>
                  {courses.map((c) => (
                    <option key={c.id} value={c.id}>{c.courseCode} - {c.courseName}</option>
                  ))}
                </select>
              </Field>
              <Field label="Teacher">
                <select required value={form.teacherId} onChange={(e) => setForm({ ...form, teacherId: Number(e.target.value) })} className={inputClass}>
                  <option value="">Select teacher</option>
                  {teachers.map((t) => (
                    <option key={t.id} value={t.id}>{t.firstName} {t.lastName} ({t.employeeId})</option>
                  ))}
                </select>
              </Field>
              <Field label="Semester">
                <input type="number" min={1} max={12} required value={form.semester} onChange={(e) => setForm({ ...form, semester: Number(e.target.value) })} className={inputClass} />
              </Field>
              <Field label="Credits">
                <input type="number" min={1} required value={form.credits} onChange={(e) => setForm({ ...form, credits: Number(e.target.value) })} className={inputClass} />
              </Field>
            </div>
            {formError && <p className="text-sm text-brick">{formError}</p>}
            <div className="flex flex-col-reverse justify-end gap-3 pt-2 sm:flex-row">
              <button type="button" onClick={() => setModalOpen(false)} className="rounded-md border border-parchment-line px-4 py-2 text-sm text-slate-dim hover:text-ink">
                Cancel
              </button>
              <button type="submit" disabled={saving} className="flex items-center justify-center gap-2 rounded-md bg-brass px-4 py-2 text-sm font-medium text-white hover:bg-brass-bright disabled:opacity-60">
                {saving && <Loader2 size={14} className="animate-spin" />}
                {editing ? 'Update Subject' : 'Create Subject'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </Layout>
  )
}