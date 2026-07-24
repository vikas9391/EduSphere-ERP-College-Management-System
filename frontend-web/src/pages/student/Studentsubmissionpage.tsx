// src/pages/student/StudentSubmissionPage.tsx
import { useEffect, useMemo, useState } from 'react'
import { Layout } from '@/components/Layout'
import { useAuthStore } from '@/store/authStore'
import {
  FileText,
  Clock,
  CheckCircle2,
  BarChart3,
  CalendarClock,
  BookOpen,
  Award,
  Link as LinkIcon,
  Inbox,
} from 'lucide-react'
import { getSubmissions, getAssignments, type AssignmentSubmission, type Assignment } from '@/api/assignment'

function isEvaluated(status: string) {
  return status.toUpperCase() === 'EVALUATED'
}

function statusClasses(status: string) {
  if (isEvaluated(status)) return 'bg-green-100 text-green-700'
  if (status.toUpperCase() === 'LATE') return 'bg-red-100 text-red-700'
  return 'bg-amber-100 text-amber-700'
}

export function StudentSubmissionPage() {
  const { user } = useAuthStore()
  const [submissions, setSubmissions] = useState<AssignmentSubmission[]>([])
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let mounted = true
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const [subs, assigns] = await Promise.all([getSubmissions(), getAssignments()])
        if (mounted) {
          setSubmissions(subs.filter((s) => s.studentId === user?.id))
          setAssignments(assigns)
        }
      } catch {
        if (mounted) setError('Failed to load your submissions. Please try again.')
      } finally {
        if (mounted) setLoading(false)
      }
    }
    load()
    return () => {
      mounted = false
    }
  }, [user?.id])

  const assignmentById = useMemo(() => {
    const m = new Map<number, Assignment>()
    assignments.forEach((a) => m.set(a.id, a))
    return m
  }, [assignments])

  const stats = useMemo(() => {
    const total = submissions.length
    const pending = submissions.filter((s) => !isEvaluated(s.status)).length
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
    return { total, pending, graded: evaluated.length, avg }
  }, [submissions, assignmentById])

  const sorted = useMemo(
    () => [...submissions].sort((a, b) => (a.submittedAt < b.submittedAt ? 1 : -1)),
    [submissions],
  )

  return (
    <Layout>
      <h1 className="font-display text-2xl font-medium text-ink">My Submissions</h1>
      <p className="mt-1 text-sm text-slate-dim">Everything you've uploaded, and how it was graded</p>

      {/* Dashboard cards */}
      <div className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-4">
        <div className="rounded-lg border border-parchment-line bg-white/60 p-5">
          <div className="flex items-center gap-2 text-slate-dim">
            <FileText size={16} className="text-brass" />
            <p className="text-sm">Total Uploaded</p>
          </div>
          <p className="mt-2 text-3xl font-semibold text-ink">{stats.total}</p>
        </div>
        <div className="rounded-lg border border-parchment-line bg-white/60 p-5">
          <div className="flex items-center gap-2 text-slate-dim">
            <Clock size={16} className="text-brass" />
            <p className="text-sm">Awaiting Grading</p>
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

      {/* List */}
      <div className="mt-8">
        {loading ? (
          <div className="p-10 text-center text-sm text-slate-dim">Loading your submissions...</div>
        ) : error ? (
          <div className="p-10 text-center text-sm text-red-600">{error}</div>
        ) : sorted.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-2 rounded-lg border border-parchment-line bg-white/60 p-12 text-center">
            <Inbox size={32} className="text-slate-dim/50" />
            <p className="font-display text-base font-medium text-ink">No submissions yet</p>
            <p className="text-sm text-slate-dim">Assignments you upload will show up here.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {sorted.map((s) => {
              const a = assignmentById.get(s.assignmentId)
              const evaluated = isEvaluated(s.status)
              return (
                <div key={s.id} className="rounded-lg border border-parchment-line bg-white/60 p-4">
                  <div className="flex items-start justify-between gap-2">
                    <p className="font-display text-sm font-medium text-ink">{s.assignmentTitle}</p>
                    <span className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${statusClasses(s.status)}`}>
                      {s.status}
                    </span>
                  </div>

                  {a && <p className="mt-1 text-xs text-slate-dim">{a.subjectName}</p>}

                  <div className="mt-3 flex flex-wrap items-center gap-4 text-xs text-slate-dim">
                    <span className="inline-flex items-center gap-1">
                      <CalendarClock size={12} />
                      Submitted {s.submittedAt.slice(0, 10)}
                    </span>
                    {a && (
                      <span className="inline-flex items-center gap-1">
                        <Award size={12} />
                        {a.maxMarks} max marks
                      </span>
                    )}
                  </div>

                  <a
                    href={s.submissionUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="mt-3 inline-flex items-center gap-1 text-xs text-brass hover:underline"
                  >
                    <LinkIcon size={12} />
                    View my upload
                  </a>

                  <div className="mt-3 border-t border-parchment-line pt-3">
                    {evaluated ? (
                      <>
                        <p className="text-sm font-medium text-ink">
                          Marks: {s.marks}
                          {a ? `/${a.maxMarks}` : ''}
                        </p>
                        {s.feedback ? (
                          <p className="mt-1 text-sm text-slate-dim">{s.feedback}</p>
                        ) : (
                          <p className="mt-1 text-sm text-slate-dim">No feedback left for this submission.</p>
                        )}
                      </>
                    ) : (
                      <p className="inline-flex items-center gap-1 text-sm text-slate-dim">
                        <BookOpen size={14} />
                        Waiting for your teacher to grade this.
                      </p>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </Layout>
  )
}