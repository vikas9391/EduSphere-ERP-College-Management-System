// src/pages/MarksEntryPage.tsx
import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
import { StampGrid } from '@/components/motion'
import { StatCard, PanelError, STAT_SHADES } from '@/components/PageBits'
import {
  getExamSchedule,
  getMarksByExamSchedule,
  enterMarks,
  updateMarks,
  publishMarks,
  publishMarksForExamSchedule,
  deleteMarks,
  getStudents,
  type ExamSchedule,
  type Marks,
  type MarksPayload,
  type Student,
} from '@/api'
import { ArrowLeft, Plus, Pencil, Trash2, Loader2, CheckCircle2, ClipboardList, FileCheck2, FileClock, Percent } from 'lucide-react'

const emptyForm = {
  studentId: '' as number | '',
  internalMarks: 0,
  externalMarks: 0,
}
type FormState = typeof emptyForm

const EASE_STAMP = [0.16, 1, 0.3, 1] as const

const panelIn = {
  hidden: { opacity: 0, y: 14 },
  show: { opacity: 1, y: 0, transition: { duration: 0.4, ease: EASE_STAMP } },
}

export function MarksEntryPage() {
  const { scheduleId } = useParams()
  const id = Number(scheduleId)

  const [schedule, setSchedule] = useState<ExamSchedule | null>(null)
  const [marksList, setMarksList] = useState<Marks[]>([])
  const [students, setStudents] = useState<Student[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [publishing, setPublishing] = useState(false)

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Marks | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [sch, m, s] = await Promise.all([
        getExamSchedule(id),
        getMarksByExamSchedule(id),
        getStudents(),
      ])
      setSchedule(sch)
      setMarksList(m)
      setStudents(s)
    } catch {
      setError('Could not load marks for this schedule.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  const enteredStudentIds = new Set(marksList.map((m) => m.studentId))
  const availableStudents = students.filter((s) => !enteredStudentIds.has(s.id))

  const publishedCount = marksList.filter((m) => m.status === 'PUBLISHED').length
  const draftCount = marksList.filter((m) => m.status === 'DRAFT').length
  const averagePercent = useMemo(() => {
    if (marksList.length === 0) return 0
    const total = marksList.reduce((sum, m) => sum + (m.maxMarks > 0 ? (m.totalMarks / m.maxMarks) * 100 : 0), 0)
    return Math.round(total / marksList.length)
  }, [marksList])

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setFormError(null)
    setModalOpen(true)
  }

  function openEdit(m: Marks) {
    setEditing(m)
    setForm({
      studentId: m.studentId,
      internalMarks: m.internalMarks,
      externalMarks: m.externalMarks,
    })
    setFormError(null)
    setModalOpen(true)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!editing && !form.studentId) {
      setFormError('Please select a student.')
      return
    }
    if (schedule && form.internalMarks + form.externalMarks > schedule.maxMarks) {
      setFormError(`Total marks cannot exceed ${schedule.maxMarks}.`)
      return
    }
    setSaving(true)
    setFormError(null)
    const payload: MarksPayload = {
      examScheduleId: id,
      studentId: editing ? editing.studentId : Number(form.studentId),
      internalMarks: form.internalMarks,
      externalMarks: form.externalMarks,
    }
    try {
      if (editing) await updateMarks(editing.id, payload)
      else await enterMarks(payload)
      setModalOpen(false)
      await load()
    } catch {
      setFormError('Could not save these marks. They may already be published or entered.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(markId: number) {
    if (!confirm('Delete this marks entry? This cannot be undone.')) return
    try {
      await deleteMarks(markId)
      await load()
    } catch {
      alert('Could not delete this entry — it may already be published.')
    }
  }

  async function handlePublishAll() {
    if (!confirm('Publish all marks for this schedule? Published marks cannot be edited or deleted.')) return
    setPublishing(true)
    try {
      await publishMarksForExamSchedule(id)
      await load()
    } catch {
      alert('Could not publish marks for this schedule.')
    } finally {
      setPublishing(false)
    }
  }

  async function handlePublishOne(markId: number) {
    try {
      await publishMarks(markId)
      await load()
    } catch {
      alert('Could not publish this entry.')
    }
  }

  return (
    <Layout>
      <Link to={schedule ? `/exams/${schedule.examId}/schedule` : '/exams'} className="mb-4 inline-flex items-center gap-1.5 text-sm text-slate-dim hover:text-ink">
        <ArrowLeft size={14} />
        Back to schedule
      </Link>

      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-medium text-ink">
            {schedule ? `${schedule.subjectName} Marks` : 'Marks Entry'}
          </h1>
          <p className="mt-1 text-sm text-slate-dim">
            {schedule ? `${schedule.examName} · Max marks ${schedule.maxMarks}` : 'Loading…'}
          </p>
        </div>
        <div className="flex gap-3">
          <button
            onClick={openCreate}
            className="flex items-center gap-2 rounded-md bg-brass px-4 py-2 text-sm font-medium text-ink hover:bg-brass-bright"
          >
            <Plus size={16} />
            Add Marks
          </button>
          <button
            onClick={handlePublishAll}
            disabled={publishing || draftCount === 0}
            className="flex items-center gap-2 rounded-md border border-brass px-4 py-2 text-sm font-medium text-ink hover:bg-brass/10 disabled:opacity-50"
          >
            {publishing ? <Loader2 size={14} className="animate-spin" /> : <CheckCircle2 size={16} />}
            Publish All ({draftCount} draft{draftCount === 1 ? '' : 's'})
          </button>
        </div>
      </div>

      <StampGrid className="mb-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatCard icon={ClipboardList} label="Total Entries" value={marksList.length} accent={STAT_SHADES[0]} />
        <StatCard icon={FileCheck2} label="Published" value={publishedCount} accent={STAT_SHADES[3]} />
        <StatCard icon={FileClock} label="Draft" value={draftCount} accent={STAT_SHADES[5]} />
        <StatCard icon={Percent} label="Average" value={averagePercent} suffix="%" accent={STAT_SHADES[6]} />
      </StampGrid>

      {loading ? (
        <p className="text-sm text-slate-dim">Loading…</p>
      ) : error ? (
        <PanelError message={error} />
      ) : marksList.length === 0 ? (
        <div className="rounded-lg border border-dashed border-parchment-line bg-white/60 p-10 text-center">
          <h3 className="font-display text-xl">No Marks Entered</h3>
          <p className="mt-2 text-slate-dim">Add marks for students who sat this exam.</p>
          <button onClick={openCreate} className="mt-5 rounded-md bg-brass px-5 py-2">
            Add Marks
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
                <th className="px-4 py-3 font-medium">Student</th>
                <th className="px-4 py-3 font-medium">Internal</th>
                <th className="px-4 py-3 font-medium">External</th>
                <th className="px-4 py-3 font-medium">Total</th>
                <th className="px-4 py-3 font-medium">Grade</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {marksList.map((m) => (
                <tr key={m.id} className="border-b border-parchment-line last:border-0">
                  <td className="px-4 py-3 text-ink">{m.studentName}</td>
                  <td className="px-4 py-3 text-slate-dim">{m.internalMarks}</td>
                  <td className="px-4 py-3 text-slate-dim">{m.externalMarks}</td>
                  <td className="px-4 py-3 text-slate-dim">{m.totalMarks} / {m.maxMarks}</td>
                  <td className="px-4 py-3">
                    <span className={`rounded px-2 py-1 text-xs ${m.grade === 'F' ? 'bg-brick/10 text-brick' : 'bg-green-100 text-green-700'}`}>
                      {m.grade}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <span className={`rounded px-2 py-1 text-xs ${m.status === 'PUBLISHED' ? 'bg-blue-100 text-blue-700' : 'bg-parchment-dim text-slate-dim'}`}>
                      {m.status}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      {m.status === 'DRAFT' && (
                        <>
                          <button title="Publish" onClick={() => handlePublishOne(m.id)} className="rounded p-2 hover:bg-blue-50">
                            <CheckCircle2 size={16} />
                          </button>
                          <button title="Edit" onClick={() => openEdit(m)} className="rounded p-2 hover:bg-brass/10">
                            <Pencil size={16} />
                          </button>
                          <button title="Delete" onClick={() => handleDelete(m.id)} className="rounded p-2 hover:bg-brick/10">
                            <Trash2 size={16} />
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </motion.div>
      )}

      {modalOpen && (
        <Modal title={editing ? 'Update Marks' : 'Add Marks'} onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="col-span-2">
                <Field label="Student">
                  {editing ? (
                    <input disabled value={editing.studentName} className={inputClass} />
                  ) : (
                    <select required value={form.studentId} onChange={(e) => setForm({ ...form, studentId: Number(e.target.value) })} className={inputClass}>
                      <option value="">Select student</option>
                      {availableStudents.map((s) => (
                        <option key={s.id} value={s.id}>{s.firstName} {s.lastName} ({s.admissionNo})</option>
                      ))}
                    </select>
                  )}
                </Field>
              </div>
              <Field label="Internal marks">
                <input type="number" min={0} required value={form.internalMarks} onChange={(e) => setForm({ ...form, internalMarks: Number(e.target.value) })} className={inputClass} />
              </Field>
              <Field label="External marks">
                <input type="number" min={0} required value={form.externalMarks} onChange={(e) => setForm({ ...form, externalMarks: Number(e.target.value) })} className={inputClass} />
              </Field>
            </div>
            {formError && <p className="text-sm text-brick">{formError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => setModalOpen(false)} className="rounded-md border border-parchment-line px-4 py-2 text-sm text-slate-dim hover:text-ink">
                Cancel
              </button>
              <button type="submit" disabled={saving} className="flex items-center gap-2 rounded-md bg-brass px-4 py-2 text-sm font-medium text-ink hover:bg-brass-bright disabled:opacity-60">
                {saving && <Loader2 size={14} className="animate-spin" />}
                {editing ? 'Update Marks' : 'Save Marks'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </Layout>
  )
}