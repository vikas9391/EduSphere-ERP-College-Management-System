import { useEffect, useMemo, useState, type FormEvent } from 'react'
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
import { getMyTeachingClassSubjects, type ClassSubject } from '@/api/schoolClass'
import { ArrowLeft, Plus, Pencil, Trash2, Loader2, ClipboardList, CalendarClock, UserCheck, Award } from 'lucide-react'

const emptyForm = {
  classSubjectId: '' as number | '',
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
  const [classSubjects, setClassSubjects] = useState<ClassSubject[]>([])
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
      const [e, s, subs, offerings, tchrs] = await Promise.all([
        getExam(id),
        getScheduleByExam(id),
        getSubjects(),
        getMyTeachingClassSubjects(),
        getTeachers(),
      ])
      setExam(e)
      setSchedules(s)
      setSubjects(subs)
      setClassSubjects(offerings)
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

  const eligibleClassSubjects = useMemo(() => {
    if (!exam) return []
    const subjectById = new Map(subjects.map((subject) => [subject.id, subject]))
    return classSubjects.filter((cs) => {
      if (cs.linkedSubjectId == null) return false
      const formalSubject = subjectById.get(cs.linkedSubjectId)
      return formalSubject?.courseId === exam.courseId
        && formalSubject.semester === exam.semester
        && (cs.semester == null || cs.semester === exam.semester)
        && (cs.academicYear == null || cs.academicYear === exam.academicYear)
    })
  }, [classSubjects, exam, subjects])

  const selectedClassSubject = useMemo(
    () => eligibleClassSubjects.find((cs) => cs.id === Number(form.classSubjectId)),
    [eligibleClassSubjects, form.classSubjectId],
  )

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setFormError(null)
    setModalOpen(true)
  }

  function openEdit(schedule: ExamSchedule) {
    setEditing(schedule)
    setForm({
      classSubjectId: schedule.classSubjectId ?? '',
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

  function selectClassSubject(value: string) {
    const classSubjectId = value ? Number(value) : ''
    const selected = eligibleClassSubjects.find((cs) => cs.id === classSubjectId)
    setForm((current) => ({
      ...current,
      classSubjectId,
      subjectId: selected?.linkedSubjectId ?? '',
    }))
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!form.classSubjectId || !form.subjectId) {
      setFormError('Please select the exact class subject for this exam slot.')
      return
    }
    if (!form.examDate || !form.startTime || !form.endTime) {
      setFormError('Exam date, start time, and end time are required.')
      return
    }
    if (form.endTime <= form.startTime) {
      setFormError('End time must be after start time.')
      return
    }
    if (form.maxMarks <= 0) {
      setFormError('Maximum marks must be greater than zero.')
      return
    }

    setSaving(true)
    setFormError(null)
    const payload: ExamSchedulePayload = {
      examId: id,
      subjectId: Number(form.subjectId),
      classSubjectId: Number(form.classSubjectId),
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
      setFormError('Could not save this slot. Check the class subject, date, and duplicate schedule rules.')
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
      <Link to="/exams" className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-text">
        <ArrowLeft size={14} />
        Back to exams
      </Link>

      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="font-heading text-2xl font-medium text-text">
            {exam ? exam.examName : 'Exam Schedule'}
          </h1>
          <p className="mt-1 text-sm text-muted">
            {exam ? `${exam.courseName} · Semester ${exam.semester} · ${exam.academicYear}` : 'Loading…'}
          </p>
        </div>
        <button
          onClick={openCreate}
          className="flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-secondary"
        >
          <Plus size={16} />
          Add Class Subject Slot
        </button>
      </div>

      <StampGrid className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard icon={CalendarClock} label="Subject Slots" value={schedules.length} accent={STAT_SHADES[0]} />
        <StatCard icon={UserCheck} label="With Invigilator" value={withInvigilator} accent={STAT_SHADES[3]} />
        <StatCard icon={Award} label="Total Marks Pool" value={totalMarksPool} accent={STAT_SHADES[6]} />
      </StampGrid>

      {loading ? (
        <p className="text-sm text-muted">Loading…</p>
      ) : error ? (
        <PanelError message={error} />
      ) : schedules.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border bg-white/60 p-10 text-center">
          <h3 className="font-heading text-xl">No Class Subjects Scheduled</h3>
          <p className="mt-2 text-muted">Add exact class-subject slots to build the exam timetable and marks roster.</p>
          <button onClick={openCreate} className="mt-5 rounded-md bg-primary px-5 py-2">
            Add Class Subject Slot
          </button>
        </div>
      ) : (
        <motion.div
          className="leaf-card overflow-hidden rounded-lg border border-border bg-white/50 shadow-[var(--shadow-card-hover)]"
          variants={panelIn}
          initial="hidden"
          animate="show"
        >
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-border text-xs uppercase tracking-wide text-muted">
                <th className="px-4 py-3 font-medium">Class / Subject</th>
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
                <tr key={s.id} className="border-b border-border last:border-0">
                  <td className="px-4 py-3">
                    <div className="text-text">{s.subjectName}</div>
                    <div className="text-xs text-muted">{s.className || 'Legacy subject-only schedule'}</div>
                  </td>
                  <td className="px-4 py-3 text-muted">{s.examDate}</td>
                  <td className="px-4 py-3 text-muted">{s.startTime} – {s.endTime}</td>
                  <td className="px-4 py-3 text-muted">{s.room || '—'}</td>
                  <td className="px-4 py-3 text-muted">{s.invigilatorName || '—'}</td>
                  <td className="px-4 py-3">
                    <span className="rounded bg-green-100 px-2 py-1 text-xs text-green-700">{s.maxMarks} marks</span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      <Link to={`/exam-schedules/${s.id}/marks`} title="Enter marks" className="rounded p-2 hover:bg-border/60">
                        <ClipboardList size={16} />
                      </Link>
                      <button title="Edit" onClick={() => openEdit(s)} className="rounded p-2 hover:bg-primary/10">
                        <Pencil size={16} />
                      </button>
                      <button title="Delete" onClick={() => handleDelete(s.id)} className="rounded p-2 hover:bg-danger/10">
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
        <Modal title={editing ? 'Update Schedule Slot' : 'Add Class Subject Slot'} onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit} className="space-y-4">
            <Field label="Class Subject">
              <select
                required
                value={form.classSubjectId}
                onChange={(e) => selectClassSubject(e.target.value)}
                className={inputClass}
              >
                <option value="">Select the exact class subject</option>
                {eligibleClassSubjects.map((cs) => (
                  <option key={cs.id} value={cs.id}>
                    {cs.schoolClassName || `Class ${cs.schoolClassId}`} · {cs.subjectCode} - {cs.subjectName} · {cs.teacherName}
                  </option>
                ))}
              </select>
            </Field>

            {editing && !editing.classSubjectId && (
              <p className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
                This is a legacy subject-only exam slot. Select its exact class subject before saving so marks are tied to one roster.
              </p>
            )}

            {eligibleClassSubjects.length === 0 && (
              <p className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
                No linked class subjects match this exam's course, semester, and academic year. Configure class subjects first.
              </p>
            )}

            <div className="grid grid-cols-2 gap-4">
              <Field label="Formal Subject">
                <input readOnly value={selectedClassSubject?.linkedSubjectName || ''} className={inputClass} placeholder="Selected automatically" />
              </Field>
              <Field label="Class Teacher">
                <input readOnly value={selectedClassSubject?.teacherName || ''} className={inputClass} placeholder="Selected automatically" />
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
            {formError && <p className="text-sm text-danger">{formError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => setModalOpen(false)} className="rounded-md border border-border px-4 py-2 text-sm text-muted hover:text-text">
                Cancel
              </button>
              <button type="submit" disabled={saving || eligibleClassSubjects.length === 0} className="flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-secondary disabled:opacity-60">
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
