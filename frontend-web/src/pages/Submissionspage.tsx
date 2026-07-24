// src/pages/SubmissionsPage.tsx
import { useEffect, useMemo, useState } from 'react'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
import {
  FileText,
  Clock,
  CheckCircle2,
  BarChart3,
  Search,
  Filter,
  X,
  Eye,
  Download,
  GraduationCap,
  BookOpen,
  CalendarClock,
  Link as LinkIcon,
} from 'lucide-react'
import {
  getSubmissions,
  getAssignments,
  evaluateSubmission,
  type AssignmentSubmission,
  type Assignment,
} from '@/api/assignment'
import { getSubjects, type Subject } from '@/api/subject'

function isEvaluated(status: string) {
  return status.toUpperCase() === 'EVALUATED'
}

function letterGrade(marks: number, maxMarks: number) {
  if (!maxMarks) return '—'
  const pct = (marks / maxMarks) * 100
  if (pct >= 90) return 'A+'
  if (pct >= 80) return 'A'
  if (pct >= 70) return 'B'
  if (pct >= 60) return 'C'
  if (pct >= 50) return 'D'
  return 'F'
}

function statusClasses(status: string) {
  if (isEvaluated(status)) return 'bg-green-100 text-green-700'
  if (status.toUpperCase() === 'LATE') return 'bg-red-100 text-red-700'
  return 'bg-amber-100 text-amber-700'
}

export function SubmissionsPage() {
  const [submissions, setSubmissions] = useState<AssignmentSubmission[]>([])
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [subjects, setSubjects] = useState<Subject[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [search, setSearch] = useState('')
  const [subjectFilter, setSubjectFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')

  const [viewing, setViewing] = useState<AssignmentSubmission | null>(null)
  const [grading, setGrading] = useState<AssignmentSubmission | null>(null)
  const [marksInput, setMarksInput] = useState('')
  const [feedbackInput, setFeedbackInput] = useState('')
  const [saving, setSaving] = useState(false)
  const [gradeError, setGradeError] = useState<string | null>(null)

  async function loadAll() {
    setLoading(true)
    setError(null)
    try {
      const [subs, assigns, subs2] = await Promise.all([getSubmissions(), getAssignments(), getSubjects()])
      setSubmissions(subs)
      setAssignments(assigns)
      setSubjects(subs2)
    } catch {
      setError('Failed to load submissions. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAll()
  }, [])

  const assignmentById = useMemo(() => {
    const m = new Map<number, Assignment>()
    assignments.forEach((a) => m.set(a.id, a))
    return m
  }, [assignments])

  const statusOptions = useMemo(() => {
    const set = new Set<string>()
    submissions.forEach((s) => set.add(s.status))
    return Array.from(set)
  }, [submissions])

  const stats = useMemo(() => {
    const total = submissions.length
    const pending = submissions.filter((s) => !isEvaluated(s.status)).length
    const graded = submissions.filter((s) => isEvaluated(s.status)).length
    const evaluated = submissions.filter((s) => isEvaluated(s.status))
    const avg =
      evaluated.length === 0
        ? 0
        : Math.round(
            evaluated.reduce((sum, s) => {
              const a = assignmentById.get(s.assignmentId)
              const max = a?.maxMarks || 1
              return sum + (s.marks / max) * 100
            }, 0) / evaluated.length,
          )
    return { total, pending, graded, avg }
  }, [submissions, assignmentById])

  const filtered = useMemo(() => {
    return submissions.filter((s) => {
      const a = assignmentById.get(s.assignmentId)
      const matchesSearch =
        !search ||
        s.studentName.toLowerCase().includes(search.toLowerCase()) ||
        s.assignmentTitle.toLowerCase().includes(search.toLowerCase())
      const matchesSubject = !subjectFilter || (a && String(a.subjectId) === subjectFilter)
      const matchesStatus = !statusFilter || s.status === statusFilter
      return matchesSearch && matchesSubject && matchesStatus
    })
  }, [submissions, search, subjectFilter, statusFilter, assignmentById])

  const hasFilters = !!search || !!subjectFilter || !!statusFilter

  function openGrade(s: AssignmentSubmission) {
    setGrading(s)
    setMarksInput(s.marks ? String(s.marks) : '')
    setFeedbackInput(s.feedback || '')
    setGradeError(null)
  }

  function closeGrade() {
    setGrading(null)
    setMarksInput('')
    setFeedbackInput('')
    setGradeError(null)
  }

  async function handleSaveGrade(e: React.FormEvent) {
    e.preventDefault()
    if (!grading) return
    const marks = Number(marksInput)
    if (marksInput === '' || Number.isNaN(marks) || marks < 0) {
      setGradeError('Please enter a valid marks value.')
      return
    }
    const assignment = assignmentById.get(grading.assignmentId)
    if (assignment && marks > assignment.maxMarks) {
      setGradeError(`Marks cannot exceed ${assignment.maxMarks}.`)
      return
    }
    setSaving(true)
    setGradeError(null)
    try {
      const updated = await evaluateSubmission(grading.id, marks, feedbackInput)
      setSubmissions((prev) => prev.map((s) => (s.id === grading.id ? updated : s)))
      closeGrade()
    } catch {
      setGradeError('Failed to save the grade. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  function handleDownload(s: AssignmentSubmission) {
    window.open(s.submissionUrl, '_blank', 'noreferrer')
  }

  return (
    <Layout>
      <h1 className="font-display text-2xl font-medium text-ink">Submissions</h1>
      <p className="mt-1 text-sm text-slate-dim">Review and grade student assignment submissions</p>

      {/* Dashboard cards */}
      <div className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-4">
        <div className="rounded-lg border border-parchment-line bg-white/60 p-5">
          <div className="flex items-center gap-2 text-slate-dim">
            <FileText size={16} className="text-brass" />
            <p className="text-sm">Total Submissions</p>
          </div>
          <p className="mt-2 text-3xl font-semibold text-ink">{stats.total}</p>
        </div>
        <div className="rounded-lg border border-parchment-line bg-white/60 p-5">
          <div className="flex items-center gap-2 text-slate-dim">
            <Clock size={16} className="text-brass" />
            <p className="text-sm">Pending Grading</p>
          </div>
          <p className="mt-2 text-3xl font-semibold text-ink">{stats.pending}</p>
        </div>
        <div className="rounded-lg border border-parchment-line bg-white/60 p-5">
          <div className="flex items-center gap-2 text-slate-dim">
            <CheckCircle2 size={16} className="text-brass" />
            <p className="text-sm">Graded</p>
          </div>
          <p className="mt-2 text-3xl font-semibold text-ink">{stats.graded}</p>
        </div>
        <div className="rounded-lg border border-parchment-line bg-white/60 p-5">
          <div className="flex items-center gap-2 text-slate-dim">
            <BarChart3 size={16} className="text-brass" />
            <p className="text-sm">Average Score</p>
          </div>
          <p className="mt-2 text-3xl font-semibold text-ink">{stats.avg}%</p>
        </div>
      </div>

      {/* Search + filters */}
      <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-dim" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by student or assignment..."
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
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="rounded-lg border border-parchment-line bg-white/60 px-3 py-2 text-sm text-ink focus:border-brass focus:outline-none"
          >
            <option value="">All Statuses</option>
            {statusOptions.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>

          {hasFilters && (
            <button
              onClick={() => {
                setSearch('')
                setSubjectFilter('')
                setStatusFilter('')
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
          <div className="p-10 text-center text-sm text-slate-dim">Loading submissions...</div>
        ) : error ? (
          <div className="p-10 text-center text-sm text-red-600">{error}</div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-2 p-12 text-center">
            <FileText size={32} className="text-slate-dim/50" />
            <p className="font-display text-base font-medium text-ink">
              {hasFilters ? 'No submissions match your filters' : 'No submissions yet'}
            </p>
            <p className="text-sm text-slate-dim">
              {hasFilters ? 'Try adjusting your search or filters.' : 'Student submissions will appear here.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-240 text-left text-sm">
              <thead>
                <tr className="border-b border-parchment-line text-slate-dim">
                  <th className="px-5 py-3 font-medium">Student</th>
                  <th className="px-5 py-3 font-medium">Assignment</th>
                  <th className="px-5 py-3 font-medium">Submitted On</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                  <th className="px-5 py-3 font-medium">Marks</th>
                  <th className="px-5 py-3 font-medium">Grade</th>
                  <th className="px-5 py-3 font-medium text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((s) => {
                  const a = assignmentById.get(s.assignmentId)
                  const evaluated = isEvaluated(s.status)
                  return (
                    <tr key={s.id} className="border-b border-parchment-line last:border-0 hover:bg-white/80">
                      <td className="px-5 py-3 text-ink">{s.studentName}</td>
                      <td className="px-5 py-3 text-slate-dim">{s.assignmentTitle}</td>
                      <td className="px-5 py-3 text-slate-dim">{s.submittedAt.slice(0, 10)}</td>
                      <td className="px-5 py-3">
                        <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${statusClasses(s.status)}`}>
                          {s.status}
                        </span>
                      </td>
                      <td className="px-5 py-3 text-slate-dim">
                        {evaluated ? `${s.marks}/${a?.maxMarks ?? '—'}` : '—'}
                      </td>
                      <td className="px-5 py-3 text-slate-dim">
                        {evaluated && a ? letterGrade(s.marks, a.maxMarks) : '—'}
                      </td>
                      <td className="px-5 py-3">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => setViewing(s)}
                            className="rounded-md p-1.5 text-slate-dim hover:bg-parchment-line/50 hover:text-brass"
                            aria-label="View submission"
                          >
                            <Eye size={16} />
                          </button>
                          <button
                            onClick={() => handleDownload(s)}
                            className="rounded-md p-1.5 text-slate-dim hover:bg-parchment-line/50 hover:text-brass"
                            aria-label="Download submission"
                          >
                            <Download size={16} />
                          </button>
                          <button
                            onClick={() => openGrade(s)}
                            className="rounded-md p-1.5 text-slate-dim hover:bg-parchment-line/50 hover:text-brass"
                            aria-label="Grade submission"
                          >
                            <GraduationCap size={16} />
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

      {/* View modal */}
      {viewing && (
        <Modal onClose={() => setViewing(null)} title={viewing.assignmentTitle}>
          <div className="space-y-4">
            <div className="flex flex-wrap items-center gap-3 text-sm text-slate-dim">
              <span className="inline-flex items-center gap-1">
                <BookOpen size={14} />
                {viewing.studentName}
              </span>
              <span className="inline-flex items-center gap-1">
                <CalendarClock size={14} />
                Submitted {viewing.submittedAt.slice(0, 10)}
              </span>
              <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${statusClasses(viewing.status)}`}>
                {viewing.status}
              </span>
            </div>

            <a
              href={viewing.submissionUrl}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1 text-sm text-brass hover:underline"
            >
              <LinkIcon size={14} />
              Open submission link
            </a>

            {isEvaluated(viewing.status) ? (
              <div className="rounded-lg border border-parchment-line p-4">
                <p className="text-sm font-medium text-ink">
                  Marks: {viewing.marks}
                  {assignmentById.get(viewing.assignmentId) ? `/${assignmentById.get(viewing.assignmentId)!.maxMarks}` : ''}
                </p>
                {viewing.feedback && <p className="mt-1 text-sm text-slate-dim">{viewing.feedback}</p>}
              </div>
            ) : (
              <p className="text-sm text-slate-dim">This submission hasn't been graded yet.</p>
            )}

            <div className="flex justify-end gap-2 pt-1">
              <button
                onClick={() => setViewing(null)}
                className="rounded-lg border border-parchment-line px-4 py-2 text-sm text-slate-dim hover:border-brass hover:text-ink"
              >
                Close
              </button>
              <button
                onClick={() => {
                  const s = viewing
                  setViewing(null)
                  openGrade(s)
                }}
                className="inline-flex items-center gap-2 rounded-lg bg-brass px-4 py-2 text-sm font-medium text-white hover:bg-brass/90"
              >
                <GraduationCap size={16} />
                Grade
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* Grade modal */}
      {grading && (
        <Modal onClose={closeGrade} title={`Grade: ${grading.assignmentTitle}`}>
          <form onSubmit={handleSaveGrade} className="space-y-4">
            <p className="text-sm text-slate-dim">
              Student: <span className="text-ink">{grading.studentName}</span>
            </p>

            <Field label={`Marks${assignmentById.get(grading.assignmentId) ? ` (out of ${assignmentById.get(grading.assignmentId)!.maxMarks})` : ''}`}>
              <input
                type="number"
                min={0}
                value={marksInput}
                onChange={(e) => setMarksInput(e.target.value)}
                className={inputClass}
                placeholder="0"
              />
            </Field>

            <Field label="Feedback">
              <textarea
                value={feedbackInput}
                onChange={(e) => setFeedbackInput(e.target.value)}
                className={`${inputClass} min-h-24 resize-y`}
                placeholder="Feedback for the student"
              />
            </Field>

            {gradeError && <p className="text-sm text-red-600">{gradeError}</p>}

            <div className="flex justify-end gap-2 pt-1">
              <button
                type="button"
                onClick={closeGrade}
                className="rounded-lg border border-parchment-line px-4 py-2 text-sm text-slate-dim hover:border-brass hover:text-ink"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={saving}
                className="inline-flex items-center gap-2 rounded-lg bg-brass px-4 py-2 text-sm font-medium text-white hover:bg-brass/90 disabled:opacity-60"
              >
                <GraduationCap size={16} />
                {saving ? 'Saving...' : 'Save Grade'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </Layout>
  )
}