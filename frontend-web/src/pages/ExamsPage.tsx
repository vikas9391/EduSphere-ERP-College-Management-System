// src/pages/ExamsPage.tsx
import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
import { StampGrid } from '@/components/motion'
import { StatCard, PanelError, STAT_SHADES } from '@/components/PageBits'
import {
  getExams,
  createExam,
  updateExam,
  deleteExam,
  getCourses,
  type Exam,
  type ExamPayload,
  type Course,
} from '@/api'
import { Plus, Pencil, Trash2, Loader2, Search, CalendarClock, GraduationCap, BookText, FlaskConical } from 'lucide-react'

const examTypes = ['MID', 'SEMESTER', 'PRACTICAL']

const emptyForm = {
  examName: '',
  examType: 'MID',
  academicYear: '',
  semester: 1,
  courseId: '' as number | '',
  startDate: '',
  endDate: '',
}
type FormState = typeof emptyForm

const EASE_STAMP = [0.16, 1, 0.3, 1] as const

const panelIn = {
  hidden: { opacity: 0, y: 14 },
  show: { opacity: 1, y: 0, transition: { duration: 0.4, ease: EASE_STAMP } },
}

export function ExamsPage() {
  const [exams, setExams] = useState<Exam[]>([])
  const [courses, setCourses] = useState<Course[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Exam | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [e, c] = await Promise.all([getExams(), getCourses()])
      setExams(e)
      setCourses(c)
    } catch {
      setError('Could not load exams.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const filteredExams = exams.filter((exam) => {
    const value = search.toLowerCase()
    return (
      exam.examName.toLowerCase().includes(value) ||
      exam.examType.toLowerCase().includes(value) ||
      exam.courseName.toLowerCase().includes(value) ||
      exam.academicYear.toLowerCase().includes(value)
    )
  })

  const midCount = exams.filter((e) => e.examType === 'MID').length
  const semesterCount = exams.filter((e) => e.examType === 'SEMESTER').length
  const practicalCount = exams.filter((e) => e.examType === 'PRACTICAL').length

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setFormError(null)
    setModalOpen(true)
  }

  function openEdit(exam: Exam) {
    setEditing(exam)
    setForm({
      examName: exam.examName,
      examType: exam.examType,
      academicYear: exam.academicYear,
      semester: exam.semester,
      courseId: exam.courseId,
      startDate: exam.startDate,
      endDate: exam.endDate,
    })
    setFormError(null)
    setModalOpen(true)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!form.courseId) {
      setFormError('Please select a course.')
      return
    }
    if (form.endDate < form.startDate) {
      setFormError('End date cannot be before start date.')
      return
    }
    setSaving(true)
    setFormError(null)
    const payload: ExamPayload = {
      examName: form.examName,
      examType: form.examType,
      academicYear: form.academicYear,
      semester: form.semester,
      courseId: Number(form.courseId),
      startDate: form.startDate,
      endDate: form.endDate,
    }
    try {
      if (editing) await updateExam(editing.id, payload)
      else await createExam(payload)
      setModalOpen(false)
      await load()
    } catch {
      setFormError('Could not save this exam. Check the fields and try again.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('Delete this exam? This cannot be undone.')) return
    try {
      await deleteExam(id)
      await load()
    } catch {
      alert('Could not delete this exam.')
    }
  }

  return (
    <Layout>
      <div className="mb-6">
        <h1 className="font-display text-2xl font-medium text-ink">Examinations</h1>
        <p className="mt-1 text-sm text-slate-dim">Mid, semester, and practical exams by course.</p>
      </div>

      <StampGrid className="mb-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatCard icon={CalendarClock} label="Total Exams" value={exams.length} accent={STAT_SHADES[0]} />
        <StatCard icon={BookText} label="Mid" value={midCount} accent={STAT_SHADES[2]} />
        <StatCard icon={GraduationCap} label="Semester" value={semesterCount} accent={STAT_SHADES[4]} />
        <StatCard icon={FlaskConical} label="Practical" value={practicalCount} accent={STAT_SHADES[6]} />
      </StampGrid>

      <div className="mb-6 flex items-center justify-between">
        <div className="relative w-full max-w-md">
          <Search size={18} className="absolute left-3 top-3 text-slate-dim" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search exams..."
            className="w-full rounded-md border border-parchment-line bg-white pl-10 pr-4 py-2"
          />
        </div>

        <button
          onClick={openCreate}
          className="flex items-center gap-2 rounded-md bg-brass px-4 py-2 text-sm font-medium text-ink hover:bg-brass-bright"
        >
          <Plus size={16} />
          Add Exam
        </button>
      </div>

      {loading ? (
        <p className="text-sm text-slate-dim">Loading…</p>
      ) : error ? (
        <PanelError message={error} />
      ) : filteredExams.length === 0 ? (
        <div className="rounded-lg border border-dashed border-parchment-line bg-white/60 p-10 text-center">
          <h3 className="font-display text-xl">No Exams Found</h3>
          <p className="mt-2 text-slate-dim">Create your first exam.</p>
          <button onClick={openCreate} className="mt-5 rounded-md bg-brass px-5 py-2">
            Add Exam
          </button>
        </div>
      ) : (
        <motion.div
          className="paper overflow-hidden rounded-lg border border-parchment-line bg-white/50 shadow-[var(--shadow-paper-lift)]"
          variants={panelIn}
          initial="hidden"
          animate="show"
        >
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-parchment-line text-xs uppercase tracking-wide text-slate-dim">
                <th className="px-4 py-3 font-medium">Exam</th>
                <th className="px-4 py-3 font-medium">Course</th>
                <th className="px-4 py-3 font-medium">Semester</th>
                <th className="px-4 py-3 font-medium">Academic Year</th>
                <th className="px-4 py-3 font-medium">Dates</th>
                <th className="px-4 py-3 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredExams.map((e) => (
                <tr key={e.id} className="border-b border-parchment-line last:border-0">
                  <td className="px-4 py-3 text-ink">
                    <div>
                      <p className="font-medium">{e.examName}</p>
                      <span className="rounded bg-blue-100 px-2 py-0.5 text-xs text-blue-700">{e.examType}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-slate-dim">{e.courseName}</td>
                  <td className="px-4 py-3 text-slate-dim">Semester {e.semester}</td>
                  <td className="px-4 py-3 text-slate-dim">{e.academicYear}</td>
                  <td className="px-4 py-3 text-slate-dim">
                    {e.startDate} – {e.endDate}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      <Link
                        to={`/exams/${e.id}/schedule`}
                        title="Manage schedule"
                        className="rounded p-2 hover:bg-parchment-line/60"
                      >
                        <CalendarClock size={16} />
                      </Link>
                      <button
                        title="Edit"
                        onClick={() => openEdit(e)}
                        className="rounded p-2 hover:bg-brass/10"
                      >
                        <Pencil size={16} />
                      </button>
                      <button
                        title="Delete"
                        onClick={() => handleDelete(e.id)}
                        className="rounded p-2 hover:bg-brick/10"
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </motion.div>
      )}

      {modalOpen && (
        <Modal title={editing ? 'Update Exam' : 'Create Exam'} onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Field label="Exam name">
                <input required value={form.examName} onChange={(e) => setForm({ ...form, examName: e.target.value })} className={inputClass} />
              </Field>
              <Field label="Exam type">
                <select required value={form.examType} onChange={(e) => setForm({ ...form, examType: e.target.value })} className={inputClass}>
                  {examTypes.map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
              </Field>
              <Field label="Course">
                <select required value={form.courseId} onChange={(e) => setForm({ ...form, courseId: Number(e.target.value) })} className={inputClass}>
                  <option value="">Select course</option>
                  {courses.map((c) => (
                    <option key={c.id} value={c.id}>{c.courseCode} - {c.courseName}</option>
                  ))}
                </select>
              </Field>
              <Field label="Semester">
                <input type="number" min={1} max={12} required value={form.semester} onChange={(e) => setForm({ ...form, semester: Number(e.target.value) })} className={inputClass} />
              </Field>
              <Field label="Academic year">
                <input required placeholder="2025-26" value={form.academicYear} onChange={(e) => setForm({ ...form, academicYear: e.target.value })} className={inputClass} />
              </Field>
              <Field label="Start date">
                <input type="date" required value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} className={inputClass} />
              </Field>
              <Field label="End date">
                <input type="date" required value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} className={inputClass} />
              </Field>
            </div>
            {formError && <p className="text-sm text-brick">{formError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => setModalOpen(false)} className="rounded-md border border-parchment-line px-4 py-2 text-sm text-slate-dim hover:text-ink">
                Cancel
              </button>
              <button type="submit" disabled={saving} className="flex items-center gap-2 rounded-md bg-brass px-4 py-2 text-sm font-medium text-ink hover:bg-brass-bright disabled:opacity-60">
                {saving && <Loader2 size={14} className="animate-spin" />}
                {editing ? 'Update Exam' : 'Create Exam'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </Layout>
  )
}