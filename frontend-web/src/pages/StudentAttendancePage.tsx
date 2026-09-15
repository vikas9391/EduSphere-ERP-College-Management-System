import { useEffect, useState } from 'react'
import { Layout } from '@/components/Layout'
import { StampGrid } from '@/components/motion'
import { StatCard, PanelHeader, PanelError, STAT_SHADES } from '@/components/PageBits'
import { CalendarDays, CalendarOff, UserCheck, UserX, Percent, Layers } from 'lucide-react'
import { getMyAttendanceSummary, getClassHolidays, type ClassHoliday, type StudentAttendanceSummary } from '@/api/attendance'
import { getMyClassesAsStudent, type SchoolClass } from '@/api/schoolClass'

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
  const [attendance, setAttendance] = useState<StudentAttendanceSummary | null>(null)
  const [holidays, setHolidays] = useState<ClassHoliday[]>([])
  const [loading, setLoading] = useState(true)
  const [holidayLoading, setHolidayLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [holidayError, setHolidayError] = useState<string | null>(null)

  useEffect(() => {
    let mounted = true
    getMyAttendanceSummary()
      .then((summary) => { if (mounted) setAttendance(summary) })
      .catch(() => { if (mounted) setError('Failed to load your attendance. Please try again.') })
      .finally(() => { if (mounted) setLoading(false) })

    getMyClassesAsStudent()
      .then(async (classes: SchoolClass[]) => {
        if (!mounted || classes.length === 0) return
        const results = await Promise.all(classes.map((schoolClass) => getClassHolidays(schoolClass.id).catch(() => [])))
        if (mounted) {
          const unique = new Map<number, ClassHoliday>()
          results.flat().forEach((holiday) => unique.set(holiday.id, holiday))
          setHolidays(Array.from(unique.values()).sort((a, b) => b.holidayDate.localeCompare(a.holidayDate)))
        }
      })
      .catch(() => { if (mounted) setHolidayError('Holiday calendar could not be loaded.') })
      .finally(() => { if (mounted) setHolidayLoading(false) })

    return () => { mounted = false }
  }, [])

  const overall = attendance ?? { totalClasses: 0, classesAttended: 0, classesMissed: 0, overallAttendancePercentage: 0, bySubject: [] }

  return (
    <Layout>
      <h1 className="font-heading text-2xl font-medium text-text">My Attendance</h1>
      <p className="mt-1 text-sm text-muted">Overall attendance and subject-wise class attendance</p>
      <StampGrid className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard icon={Percent} label="Overall Attendance" value={overall.overallAttendancePercentage} suffix="%" accent={STAT_SHADES[0]} />
        <StatCard icon={CalendarDays} label="Total Classes" value={overall.totalClasses} accent={STAT_SHADES[2]} />
        <StatCard icon={UserCheck} label="Classes Attended" value={overall.classesAttended} accent={STAT_SHADES[4]} />
        <StatCard icon={UserX} label="Classes Not Attended" value={overall.classesMissed} accent={STAT_SHADES[6]} />
      </StampGrid>

      <div className="leaf-card mt-8 rounded-lg border border-border bg-white/60 p-5 shadow-[var(--shadow-card-hover)]">
        <PanelHeader icon={Layers} title="Subject-wise Attendance" />
        {loading ? <p className="text-center text-sm text-muted">Loading your attendance...</p> : error ? <PanelError message={error} /> : overall.bySubject.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-2 py-8 text-center"><Layers size={32} className="text-muted/50" /><p className="font-heading text-base font-medium text-text">No attendance records yet</p><p className="text-sm text-muted">Subject attendance will appear here once classes are recorded.</p></div>
        ) : (
          <div className="overflow-x-auto"><table className="w-full min-w-[700px] text-sm"><thead><tr className="border-b border-border text-left text-xs text-muted"><th className="px-3 py-3">Subject</th><th className="px-3 py-3 text-center">Attendance</th><th className="px-3 py-3 text-center">Total Classes</th><th className="px-3 py-3 text-center">Attended</th><th className="px-3 py-3 text-center">Not Attended</th></tr></thead><tbody>
            {overall.bySubject.map((s) => <tr key={`${s.subjectId ?? 'unknown'}:${s.subjectCode}:${s.subjectName}`} className="border-b border-border/60"><td className="px-3 py-4"><div className="font-medium text-text">{s.subjectName}</div><div className="text-xs text-muted">{s.subjectCode}</div></td><td className="px-3 py-4"><div className="flex items-center gap-3"><div className="h-2 flex-1 rounded-full bg-border/50"><div className={`h-full rounded-full ${progressColor(s.attendancePercentage)}`} style={{ width: `${s.attendancePercentage}%` }} /></div><span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${badgeClasses(s.attendancePercentage)}`}>{s.attendancePercentage}%</span></div></td><td className="px-3 py-4 text-center">{s.totalClasses}</td><td className="px-3 py-4 text-center font-medium">{s.classesAttended}</td><td className="px-3 py-4 text-center">{s.classesMissed}</td></tr>)}
          </tbody></table></div>
        )}
      </div>

      <div className="leaf-card mt-6 rounded-lg border border-border bg-white/60 p-5 shadow-[var(--shadow-card-hover)]">
        <PanelHeader icon={CalendarOff} title="Holiday Calendar" />
        <p className="mb-4 text-xs text-muted">Configured holidays for your enrolled classes are excluded from attendance calculations and shown as <strong className="text-text">Not Counted</strong>.</p>
        {holidayLoading ? <p className="text-center text-sm text-muted">Loading holiday calendar...</p> : holidayError ? <PanelError message={holidayError} /> : holidays.length === 0 ? (
          <div className="py-6 text-center text-sm text-muted">No class holidays have been configured.</div>
        ) : (
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">{holidays.map((holiday) => <div key={holiday.id} className="flex items-center justify-between rounded-lg border border-border bg-bg px-4 py-3"><div><p className="text-sm font-medium text-text">{new Date(`${holiday.holidayDate}T00:00:00`).toLocaleDateString()}</p><p className="text-xs text-muted">{holiday.className} · {holiday.reason || 'Holiday'}</p></div><span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600">Not Counted</span></div>)}</div>
        )}
      </div>
    </Layout>
  )
}
