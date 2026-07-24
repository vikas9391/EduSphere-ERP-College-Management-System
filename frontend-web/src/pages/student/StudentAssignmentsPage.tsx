// src/pages/student/StudentAssignmentsPage.tsx
import { useEffect, useMemo, useState } from 'react'
import { Layout } from '@/components/Layout'
import { Modal } from '@/components/Modal'
import { Field, inputClass } from '@/components/FormField'
import { StampGrid, StampItem } from '@/components/motion'
import { StatCard, STAT_SHADES } from '@/components/PageBits'
import { useAuthStore } from '@/store/authStore'
import {
  ClipboardList,
  Clock,
  CheckCircle2,
  AlertTriangle,
  CalendarClock,
  BookOpen,
  Award,
  Upload,
  Link as LinkIcon,
} from 'lucide-react'
import {
  getAssignments,
  getSubmissions,
  submitAssignment,
  type Assignment,
  type AssignmentSubmission,
} from '@/api/assignment'

type Bucket = 'pending' | 'submitted' | 'overdue'

interface AssignmentRow {
  assignment: Assignment
  submission: AssignmentSubmission | null
  bucket: Bucket
}

function todayISO() {
  return new Date().toISOString().slice(0, 10)
}

function isOverdue(dueDate: string) {
  return dueDate.slice(0, 10) < todayISO()
}

const bucketMeta: Record<Bucket, { label: string; classes: string }> = {
  pending: { label: 'Pending', classes: 'bg-amber-100 text-amber-700' },
  submitted: { label: 'Submitted', classes: 'bg-green-100 text-green-700' },
  overdue: { label: 'Overdue', classes: 'bg-red-100 text-red-700' },
}

export function StudentAssignmentsPage() {
  const { user } = useAuthStore()
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [submissions, setSubmissions] = useState<AssignmentSubmission[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [activeRow, setActiveRow] = useState<AssignmentRow | null>(null)
  const [submissionUrl, setSubmissionUrl] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  async function loadAll() {
    setLoading(true)
    setError(null)
    try {
      const [a, s] = await Promise.all([getAssignments(), getSubmissions()])
      setAssignments(a)
      setSubmissions(s.filter((sub: AssignmentSubmission) => sub.studentId === user?.id))
    } catch {
      setError('Failed to load your assignments. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAll()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id])

  const rows: AssignmentRow[] = useMemo(() => {
    return assignments.map((assignment) => {
      const submission = submissions.find((s) => s.assignmentId === assignment.id) ?? null
      let bucket: Bucket = 'pending'
      if (submission) bucket = 'submitted'
      else if (isOverdue(assignment.dueDate)) bucket = 'overdue'
      return { assignment, submission, bucket }
    })
  }, [assignments, submissions])

  const stats = useMemo(() => {
    const pending = rows.filter((r) => r.bucket === 'pending').length
    const submitted = rows.filter((r) => r.bucket === 'submitted').length
    const overdue = rows.filter((r) => r.bucket === 'overdue').length
    return { total: rows.length, pending, submitted, overdue }
  }, [rows])

  function openDetails(row: AssignmentRow) {
    setActiveRow(row)
    setSubmissionUrl('')
    setSubmitError(null)
  }

  function closeDetails() {
    setActiveRow(null)
    setSubmissionUrl('')
    setSubmitError(null)
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!activeRow || !user?.id) return
    if (!submissionUrl.trim()) {
      setSubmitError('Please provide a link to your submission.')
      return
    }
    setSubmitting(true)
    setSubmitError(null)
    try {
      await submitAssignment({
        assignmentId: activeRow.assignment.id,
        studentId: user.id,
        submissionUrl: submissionUrl.trim(),
      })
      await loadAll()
      closeDetails()
    } catch {
      setSubmitError('Failed to submit your assignment. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  const overdueRows = rows.filter((r) => r.bucket === 'overdue')
  const pendingRows = rows.filter((r) => r.bucket === 'pending')
  const submittedRows = rows.filter((r) => r.bucket === 'submitted')

  function renderCard(row: AssignmentRow) {
    return (
      <StampItem
        key={row.assignment.id}
        className="cursor-pointer rounded-lg border border-parchment-line bg-white/60 p-4 text-left transition-colors hover:border-brass"
      >
        <button onClick={() => openDetails(row)} className="w-full text-left">
          <div className="flex items-start justify-between gap-2">
            <p className="font-display text-sm font-medium text-ink">{row.assignment.title}</p>
            <span className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${bucketMeta[row.bucket].classes}`}>
              {bucketMeta[row.bucket].label}
            </span>
          </div>
          <p className="mt-1 text-xs text-slate-dim">{row.assignment.subjectName}</p>
          <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-dim">
            <span className="inline-flex items-center gap-1">
              <CalendarClock size={12} />
              Due {row.assignment.dueDate.slice(0, 10)}
            </span>
            <span className="inline-flex items-center gap-1">
              <Award size={12} />
              {row.assignment.maxMarks} marks
            </span>
          </div>
          {row.submission && row.submission.status === 'EVALUATED' && (
            <p className="mt-2 text-xs font-medium text-green-700">
              Score: {row.submission.marks}/{row.assignment.maxMarks}
            </p>
          )}
        </button>
      </StampItem>
    )
  }

  function renderSection(title: string, Icon: typeof Clock, sectionRows: AssignmentRow[]) {
    return (
      <div className="mt-8">
        <div className="flex items-center gap-2">
          <Icon size={18} className="text-brass" />
          <h2 className="font-display text-base font-medium text-ink">{title}</h2>
          <span className="rounded-full bg-parchment-line/50 px-2 py-0.5 text-xs text-slate-dim">
            {sectionRows.length}
          </span>
        </div>

        {sectionRows.length === 0 ? (
          <p className="mt-3 text-sm text-slate-dim">Nothing here.</p>
        ) : (
          <StampGrid className="mt-3 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {sectionRows.map((row) => renderCard(row))}
          </StampGrid>
        )}
      </div>
    )
  }

  return (
    <Layout>
      <h1 className="font-display text-2xl font-medium text-ink">My Assignments</h1>
      <p className="mt-1 text-sm text-slate-dim">Track and submit your assignments across all subjects</p>

      <StampGrid className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard icon={ClipboardList} label="Total Assignments" value={stats.total} accent={STAT_SHADES[0]} />
        <StatCard icon={Clock} label="Pending" value={stats.pending} accent={STAT_SHADES[2]} />
        <StatCard icon={CheckCircle2} label="Submitted" value={stats.submitted} accent={STAT_SHADES[4]} />
        <StatCard icon={AlertTriangle} label="Overdue" value={stats.overdue} accent={STAT_SHADES[6]} />
      </StampGrid>

      {loading ? (
        <div className="mt-10 p-10 text-center text-sm text-slate-dim">Loading your assignments...</div>
      ) : error ? (
        <div className="mt-10 p-10 text-center text-sm text-red-600">{error}</div>
      ) : rows.length === 0 ? (
        <div className="mt-10 flex flex-col items-center justify-center gap-2 p-12 text-center">
          <BookOpen size={32} className="text-slate-dim/50" />
          <p className="font-display text-base font-medium text-ink">No assignments yet</p>
          <p className="text-sm text-slate-dim">Assignments from your subjects will appear here.</p>
        </div>
      ) : (
        <>
          {renderSection('Overdue', AlertTriangle, overdueRows)}
          {renderSection('Pending', Clock, pendingRows)}
          {renderSection('Submitted', CheckCircle2, submittedRows)}
        </>
      )}

      {activeRow && (
        <Modal onClose={closeDetails} title={activeRow.assignment.title}>
          <div className="space-y-4">
            <div className="flex flex-wrap items-center gap-3 text-sm text-slate-dim">
              <span className="inline-flex items-center gap-1">
                <BookOpen size={14} />
                {activeRow.assignment.subjectName}
              </span>
              <span className="inline-flex items-center gap-1">
                <CalendarClock size={14} />
                Due {activeRow.assignment.dueDate.slice(0, 10)}
              </span>
              <span className="inline-flex items-center gap-1">
                <Award size={14} />
                {activeRow.assignment.maxMarks} marks
              </span>
              <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${bucketMeta[activeRow.bucket].classes}`}>
                {bucketMeta[activeRow.bucket].label}
              </span>
            </div>

            {activeRow.assignment.description && (
              <p className="whitespace-pre-wrap text-sm text-ink">{activeRow.assignment.description}</p>
            )}

            {activeRow.submission ? (
              <div className="rounded-lg border border-parchment-line p-4">
                <p className="text-sm text-slate-dim">Submitted {activeRow.submission.submittedAt.slice(0, 10)}</p>
                <a
                  href={activeRow.submission.submissionUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="mt-1 inline-flex items-center gap-1 text-sm text-brass hover:underline"
                >
                  <LinkIcon size={14} />
                  View submission
                </a>
                {activeRow.submission.status === 'EVALUATED' ? (
                  <div className="mt-3 border-t border-parchment-line pt-3">
                    <p className="text-sm font-medium text-ink">
                      Score: {activeRow.submission.marks}/{activeRow.assignment.maxMarks}
                    </p>
                    {activeRow.submission.feedback && (
                      <p className="mt-1 text-sm text-slate-dim">{activeRow.submission.feedback}</p>
                    )}
                  </div>
                ) : (
                  <p className="mt-3 border-t border-parchment-line pt-3 text-sm text-slate-dim">
                    Awaiting evaluation.
                  </p>
                )}
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-3">
                <Field label="Submission Link">
                  <input
                    value={submissionUrl}
                    onChange={(e) => setSubmissionUrl(e.target.value)}
                    className={inputClass}
                    placeholder="https://..."
                  />
                </Field>
                {submitError && <p className="text-sm text-red-600">{submitError}</p>}
                <div className="flex justify-end gap-2 pt-1">
                  <button
                    type="button"
                    onClick={closeDetails}
                    className="rounded-lg border border-parchment-line px-4 py-2 text-sm text-slate-dim hover:border-brass hover:text-ink"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={submitting}
                    className="inline-flex items-center gap-2 rounded-lg bg-brass px-4 py-2 text-sm font-medium text-white hover:bg-brass/90 disabled:opacity-60"
                  >
                    <Upload size={16} />
                    {submitting ? 'Submitting...' : 'Submit Assignment'}
                  </button>
                </div>
              </form>
            )}
          </div>
        </Modal>
      )}
    </Layout>
  )
}