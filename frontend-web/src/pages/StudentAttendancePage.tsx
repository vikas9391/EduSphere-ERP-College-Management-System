// src/pages/StudentAttendancePage.tsx
import { useEffect, useMemo, useState } from 'react'
import { Layout } from '@/components/Layout'
import { StampGrid } from '@/components/motion'
import { StatCard, PanelHeader, PanelError, STAT_SHADES } from '@/components/PageBits'
import { useAuthStore } from '@/store/authStore'
import { CalendarDays, UserCheck, UserX, Percent, Layers } from 'lucide-react'
import { getAttendance, type Attendance } from '@/api'
import { getSubjects, type Subject } from '@/api'

interface SubjectSummary {
  subjectId: number
  subjectName: string
  courseName: string
  present: number
  absent: number
  total: number
  percentage: number
}

function progressColor(pct: number) {
  if (pct >= 75) return 'bg-green-500'
  if (pct >= 50) return 'bg-amber-500'
  return 'bg-red-500'
}

function badgeClasses(pct: number) {
  if (pct >= 75) return 'bg-green-100 text-green-700'
  if (pct >= 50) return 'bg-amber-100 text-amber-700'
  return 'bg-red-100 text-red-700'
}

export function StudentAttendancePage() {
  const { user } = useAuthStore()
  const [records, setRecords] = useState<Attendance[]>([])
  const [subjects, setSubjects] = useState<Subject[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let mounted = true
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const [all, subs] = await Promise.all([getAttendance(), getSubjects()])
        // No dedicated "my attendance" endpoint anymore — filter client-side.
        const mine = all.filter((r) => r.studentId === user?.id)
        if (mounted) {
          setRecords(mine)
          setSubjects(subs)
        }
      } catch {
        if (mounted) setError('Failed to load your attendance. Please try again.')
      } finally {
        if (mounted) setLoading(false)
      }
    }
    load()
    return () => {
      mounted = false
    }
  }, [user?.id])

  const courseNameById = useMemo(() => {
    const m = new Map<number, string>()
    subjects.forEach((s) => m.set(s.id, s.courseName))
    return m
  }, [subjects])

  const overall = useMemo(() => {
    const present = records.filter((r) => r.status === 'PRESENT').length
    const absent = records.filter((r) => r.status === 'ABSENT').length
    const total = records.length
    const percentage = total ? Math.round((present / total) * 100) : 0
    return { present, absent, total, percentage }
  }, [records])

  const subjectSummaries: SubjectSummary[] = useMemo(() => {
    const map = new Map<number, SubjectSummary>()
    records.forEach((r) => {
      const existing = map.get(r.subjectId)
      if (existing) {
        existing.total += 1
        if (r.status === 'PRESENT') existing.present += 1
        else existing.absent += 1
      } else {
        map.set(r.subjectId, {
          subjectId: r.subjectId,
          subjectName: r.subjectName,
          courseName: courseNameById.get(r.subjectId) || '—',
          present: r.status === 'PRESENT' ? 1 : 0,
          absent: r.status === 'ABSENT' ? 1 : 0,
          total: 1,
          percentage: 0,
        })
      }
    })
    return Array.from(map.values())
      .map((s) => ({ ...s, percentage: s.total ? Math.round((s.present / s.total) * 100) : 0 }))
      .sort((a, b) => a.subjectName.localeCompare(b.subjectName))
  }, [records, courseNameById])

  return (
    <Layout>
      <h1 className="font-display text-2xl font-medium text-ink">My Attendance</h1>
      <p className="mt-1 text-sm text-slate-dim">
        Your attendance record across all enrolled subjects
        {user?.email ? ` · ${user.email}` : ''}
      </p>

      {/* Overall dashboard cards */}
      <StampGrid className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard icon={Percent} label="Overall Attendance" value={overall.percentage} suffix="%" accent={STAT_SHADES[0]} />
        <StatCard icon={CalendarDays} label="Total Sessions" value={overall.total} accent={STAT_SHADES[2]} />
        <StatCard icon={UserCheck} label="Present" value={overall.present} accent={STAT_SHADES[4]} />
        <StatCard icon={UserX} label="Absent" value={overall.absent} accent={STAT_SHADES[6]} />
      </StampGrid>

      {/* Subject-wise breakdown */}
      <div className="paper mt-8 rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)]">
        <PanelHeader icon={Layers} title="Subject-wise Attendance" />

        {loading ? (
          <p className="text-center text-sm text-slate-dim">Loading your attendance...</p>
        ) : error ? (
          <PanelError message={error} />
        ) : subjectSummaries.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-2 py-8 text-center">
            <Layers size={32} className="text-slate-dim/50" />
            <p className="font-display text-base font-medium text-ink">No attendance records yet</p>
            <p className="text-sm text-slate-dim">
              Your subject-wise attendance will appear here once sessions are recorded.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
            {subjectSummaries.map((s) => (
              <div key={s.subjectId} className="rounded-lg border border-parchment-line p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-display text-sm font-medium text-ink">{s.subjectName}</p>
                    <p className="text-xs text-slate-dim">{s.courseName}</p>
                  </div>
                  <span className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${badgeClasses(s.percentage)}`}>
                    {s.percentage}%
                  </span>
                </div>

                <div className="mt-3 h-2 w-full overflow-hidden rounded-full bg-parchment-line/50">
                  <div
                    className={`h-full rounded-full transition-all ${progressColor(s.percentage)}`}
                    style={{ width: `${s.percentage}%` }}
                  />
                </div>

                <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-dim">
                  <span className="inline-flex items-center gap-1">
                    <UserCheck size={12} className="text-green-600" />
                    {s.present} present
                  </span>
                  <span className="inline-flex items-center gap-1">
                    <UserX size={12} className="text-red-600" />
                    {s.absent} absent
                  </span>
                  <span>{s.total} total</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </Layout>
  )
}
