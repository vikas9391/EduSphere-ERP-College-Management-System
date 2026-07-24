// src/pages/ExamSchedulePage.tsx
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
import { StampGrid } from '@/components/motion'
import { StatCard, PanelError, STAT_SHADES } from '@/components/PageBits'
import {
  getExam,
  getScheduleByExam,
  createExamSchedule,
  updateExamSchedule,
  deleteExamSchedule,
  getSubjects,
  getTeachers,
  type Exam,
  type ExamSchedule,
  type ExamSchedulePayload,
  type Subject,
  type Teacher,
} from '@/api'
import { ArrowLeft, Plus, Pencil, Trash2, Loader2, ClipboardList, CalendarClock, UserCheck, Award } from 'lucide-react'

const emptyForm = {
  subjectId: '' as number | '',
  invigilatorId: '' as number | '',
  examDate: '',
  startTime: '',
  endTime: '',
  room: '',
  maxMarks: 100,
}
type FormState = typeof emptyForm

const EASE_STAMP = [0.16, 1, 0.3, 1] as const

const panelIn = {
  hidden: { opacity: 0, y: 14 },
  show: { opacity: 1, y: 0, transition: { duration: 0.4, ease: EASE_STAMP } },
}

export function ExamSchedulePage() {
  const { examId } = useParams()
  const id = Number(examId)

  const [exam, setExam] = useState<Exam | null>(null)
  const [schedules, setSchedules] = useState<ExamSchedule[]>([])
  const [subjects, setSubjects] = useState<Subject[]>([])
  const [teachers, setTeachers] = useState<Teacher[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<ExamSchedule | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [e, s, subs, tchrs] = await Promise.all([
        getExam(id),
        getScheduleByExam(id),
        getSubjects(),
        getTeachers(),
      ])
      setExam(e)
      setSchedules(s)
      setSubjects(subs)
      setTeachers(tchrs)
    } catch {
      setError('Could not load the exam schedule.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setFormError(null)
    setModalOpen(true)
  }

  function openEdit(schedule: ExamSchedule) {
    setEditing(schedule)
    setForm({
      subjectId: schedule.subjectId,
      invigilatorId: schedule.invigilatorId ?? '',
      examDate: schedule.examDate,
      startTime: schedule.startTime,
      endTime: schedule.endTime,
      room: schedule.room,
      maxMarks: schedule.maxMarks,
    })
    setFormError(null)
    setModalOpen(true)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!form.subjectId) {
      setFormError('Please select a subject.')
      return
    }
    if (form.endTime <= form.startTime) {
      setFormError('End time must be after start time.')
      return
    }
    setSaving(true)
    setFormError(null)
    const payload: ExamSchedulePayload = {
      examId: id,
      subjectId: Number(form.subjectId),
      invigilatorId: form.invigilatorId ? Number(form.invigilatorId) : null,
      examDate: form.examDate,
      startTime: form.startTime,
      endTime: form.endTime,
      room: form.room,
      maxMarks: form.maxMarks,
    }
    try {
      if (editing) await updateExamSchedule(editing.id, payload)
      else await createExamSchedule(payload)
      setModalOpen(false)
      await load()
    } catch {
      setFormError('Could not save this schedule slot. It may already exist for this subject.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(scheduleId: number) {
    if (!confirm('Delete this schedule slot? This cannot be undone.')) return
    try {
      await deleteExamSchedule(scheduleId)
      await load()
    } catch {
      alert('Could not delete this schedule slot.')
    }
  }

  const withInvigilator = schedules.filter((s) => s.invigilatorId).length
  const totalMarksPool = schedules.reduce((sum, s) => sum + s.maxMarks, 0)

  return (
    <Layout>
      <Link to="/exams" className="mb-4 inline-flex items-center gap-1.5 text-sm text-slate-dim hover:text-ink">
        <ArrowLeft size={14} />
        Back to exams
      </Link>

      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-medium text-ink">
            {exam ? exam.examName : 'Exam Schedule'}
          </h1>
          <p className="mt-1 text-sm text-slate-dim">
            {exam ? `${exam.courseName} · Semester ${exam.semester} · ${exam.academicYear}` : 'Loading…'}
          </p>
        </div>
        <button
          onClick={openCreate}
          className="flex items-center gap-2 rounded-md bg-brass px-4 py-2 text-sm font-medium text-ink hover:bg-brass-bright"
        >
          <Plus size={16} />
          Add Subject Slot
        </button>
      </div>

      <StampGrid className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard icon={CalendarClock} label="Subject Slots" value={schedules.length} accent={STAT_SHADES[0]} />
        <StatCard icon={UserCheck} label="With Invigilator" value={withInvigilator} accent={STAT_SHADES[3]} />
        <StatCard icon={Award} label="Total Marks Pool" value={totalMarksPool} accent={STAT_SHADES[6]} />
      </StampGrid>

      {loading ? (
        <p className="text-sm text-slate-dim">Loading…</p>
      ) : error ? (
        <PanelError message={error} />
      ) : schedules.length === 0 ? (
        <div className="rounded-lg border border-dashed border-parchment-line bg-white/60 p-10 text-center">
          <h3 className="font-display text-xl">No Subjects Scheduled</h3>
          <p className="mt-2 text-slate-dim">Add subject slots to build the exam timetable.</p>
          <button onClick={openCreate} className="mt-5 rounded-md bg-brass px-5 py-2">
            Add Subject Slot
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
                <th className="px-4 py-3 font-medium">Subject</th>
                <th className="px-4 py-3 font-medium">Date</th>
                <th className="px-4 py-3 font-medium">Time</th>
                <th className="px-4 py-3 font-medium">Room</th>
                <th className="px-4 py-3 font-medium">Invigilator</th>
                <th className="px-4 py-3 font-medium">Max Marks</th>
                <th className="px-4 py-3 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {schedules.map((s) => (
                <tr key={s.id} className="border-b border-parchment-line last:border-0">
                  <td className="px-4 py-3 text-ink">{s.subjectName}</td>
                  <td className="px-4 py-3 text-slate-dim">{s.examDate}</td>
                  <td className="px-4 py-3 text-slate-dim">{s.startTime} – {s.endTime}</td>
                  <td className="px-4 py-3 text-slate-dim">{s.room || '—'}</td>
                  <td className="px-4 py-3 text-slate-dim">{s.invigilatorName || '—'}</td>
                  <td className="px-4 py-3">
                    <span className="rounded bg-green-100 px-2 py-1 text-xs text-green-700">{s.maxMarks} marks</span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      <Link
                        to={`/exam-schedules/${s.id}/marks`}
                        title="Enter marks"
                        className="rounded p-2 hover:bg-parchment-line/60"
                      >
                        <ClipboardList size={16} />
                      </Link>
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
        </motion.div>
      )}

      {modalOpen && (
        <Modal title={editing ? 'Update Schedule Slot' : 'Add Subject Slot'} onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Field label="Subject">
                <select required value={form.subjectId} onChange={(e) => setForm({ ...form, subjectId: Number(e.target.value) })} className={inputClass}>
                  <option value="">Select subject</option>
                  {subjects.map((s) => (
                    <option key={s.id} value={s.id}>{s.subjectCode} - {s.subjectName}</option>
                  ))}
                </select>
              </Field>
              <Field label="Invigilator (optional)">
                <select value={form.invigilatorId} onChange={(e) => setForm({ ...form, invigilatorId: e.target.value ? Number(e.target.value) : '' })} className={inputClass}>
                  <option value="">None</option>
                  {teachers.map((t) => (
                    <option key={t.id} value={t.id}>{t.firstName} {t.lastName}</option>
                  ))}
                </select>
              </Field>
              <Field label="Exam date">
                <input type="date" required value={form.examDate} onChange={(e) => setForm({ ...form, examDate: e.target.value })} className={inputClass} />
              </Field>
              <Field label="Room">
                <input value={form.room} onChange={(e) => setForm({ ...form, room: e.target.value })} className={inputClass} />
              </Field>
              <Field label="Start time">
                <input type="time" required value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} className={inputClass} />
              </Field>
              <Field label="End time">
                <input type="time" required value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} className={inputClass} />
              </Field>
              <Field label="Max marks">
                <input type="number" min={1} required value={form.maxMarks} onChange={(e) => setForm({ ...form, maxMarks: Number(e.target.value) })} className={inputClass} />
              </Field>
            </div>
            {formError && <p className="text-sm text-brick">{formError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => setModalOpen(false)} className="rounded-md border border-parchment-line px-4 py-2 text-sm text-slate-dim hover:text-ink">
                Cancel
              </button>
              <button type="submit" disabled={saving} className="flex items-center gap-2 rounded-md bg-brass px-4 py-2 text-sm font-medium text-ink hover:bg-brass-bright disabled:opacity-60">
                {saving && <Loader2 size={14} className="animate-spin" />}
                {editing ? 'Update Slot' : 'Add Slot'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </Layout>
  )
}