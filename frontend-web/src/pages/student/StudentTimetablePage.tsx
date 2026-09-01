import { useEffect, useMemo, useState } from 'react'
import { Layout } from '@/components/Layout'
import { PanelError } from '@/components/PageBits'
import { getMyTimetable, type StudentTimetable, type TimetableEntry } from '@/api/studentPortal'
import { CalendarDays, Clock3, MapPin, UserRound } from 'lucide-react'

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'] as const

function dayLabel(day: string) {
  return day.charAt(0) + day.slice(1).toLowerCase()
}

export function StudentTimetablePage() {
  const [timetable, setTimetable] = useState<StudentTimetable | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let mounted = true
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const result = await getMyTimetable()
        if (mounted) setTimetable(result)
      } catch {
        if (mounted) setError('Could not load your timetable.')
      } finally {
        if (mounted) setLoading(false)
      }
    }
    load()
    return () => { mounted = false }
  }, [])

  const grouped = useMemo(() => {
    return DAYS.map((day) => ({
      day,
      entries: [...(timetable?.schedule?.[day] ?? [])].sort((a, b) => a.startTime.localeCompare(b.startTime)),
    }))
  }, [timetable])

  const totalSlots = grouped.reduce((sum, group) => sum + group.entries.length, 0)

  function renderEntry(entry: TimetableEntry, index: number) {
    return (
      <div key={`${entry.subjectId}-${entry.startTime}-${index}`} className="rounded-lg border border-border bg-white/60 p-4">
        <p className="font-heading text-sm font-medium text-text">{entry.subjectName}</p>
        <div className="mt-3 flex flex-wrap gap-x-5 gap-y-2 text-xs text-muted">
          <span className="inline-flex items-center gap-1.5">
            <Clock3 size={13} />
            {entry.startTime.slice(0, 5)} – {entry.endTime.slice(0, 5)}
          </span>
          <span className="inline-flex items-center gap-1.5">
            <UserRound size={13} />
            {entry.teacherName || 'Teacher TBD'}
          </span>
          <span className="inline-flex items-center gap-1.5">
            <MapPin size={13} />
            {entry.room || 'TBD'}
          </span>
        </div>
      </div>
    )
  }

  return (
    <Layout>
      <div>
        <h1 className="font-heading text-2xl font-medium text-text">My Timetable</h1>
        <p className="mt-1 text-sm text-muted">Your weekly schedule from your current class-subject enrollments</p>
      </div>

      {loading ? (
        <div className="mt-8 p-10 text-center text-sm text-muted">Loading timetable...</div>
      ) : error ? (
        <div className="mt-8"><PanelError message={error} /></div>
      ) : totalSlots === 0 ? (
        <div className="mt-8 rounded-lg border border-dashed border-border bg-white/60 p-10 text-center">
          <CalendarDays size={34} className="mx-auto text-muted/50" />
          <p className="mt-3 font-heading text-base font-medium text-text">No timetable slots yet</p>
          <p className="mt-1 text-sm text-muted">{timetable?.note || 'Your teachers have not scheduled class periods yet.'}</p>
        </div>
      ) : (
        <div className="mt-8 space-y-6">
          {grouped.map(({ day, entries }) => (
            <section key={day}>
              <div className="flex items-center gap-2">
                <CalendarDays size={17} className="text-primary" />
                <h2 className="font-heading text-base font-medium text-text">{dayLabel(day)}</h2>
                <span className="rounded-full bg-border/50 px-2 py-0.5 text-xs text-muted">{entries.length}</span>
              </div>
              {entries.length === 0 ? (
                <p className="mt-2 text-sm text-muted">No classes scheduled.</p>
              ) : (
                <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
                  {entries.map(renderEntry)}
                </div>
              )}
            </section>
          ))}
        </div>
      )}
    </Layout>
  )
}
