// src/pages/student/Studentenrollmentspage.tsx
import { useEffect, useMemo, useState } from 'react'
import { Layout } from '@/components/Layout'
import { StampGrid } from '@/components/motion'
import { StatCard, PanelHeader, PanelError, STAT_SHADES } from '@/components/PageBits'
import { useAuthStore } from '@/store/authStore'
import { ClipboardCheck, Search, Layers, GraduationCap, BookOpen } from 'lucide-react'
import { getMyEnrollments, type Enrollment } from '@/api/'

export function StudentEnrollmentsPage() {
  const { user } = useAuthStore()
  const [enrollments, setEnrollments] = useState<Enrollment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')

  useEffect(() => {
    let mounted = true
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const data = await getMyEnrollments()
        if (mounted) setEnrollments(data)
      } catch {
        if (mounted) setError('Failed to load your enrollments. Please try again.')
      } finally {
        if (mounted) setLoading(false)
      }
    }
    load()
    return () => {
      mounted = false
    }
  }, [])

  const filtered = useMemo(() => {
    if (!search) return enrollments
    const q = search.toLowerCase()
    return enrollments.filter(
      (e) =>
        e.subjectName.toLowerCase().includes(q) ||
        e.courseName.toLowerCase().includes(q) ||
        e.teacherName?.toLowerCase().includes(q),
    )
  }, [enrollments, search])

  const stats = useMemo(() => {
    const uniqueCourses = new Set(enrollments.map((e) => e.courseName)).size
    const uniqueTeachers = new Set(enrollments.map((e) => e.teacherName)).size
    return { total: enrollments.length, uniqueCourses, uniqueTeachers }
  }, [enrollments])

  return (
    <Layout>
      <h1 className="font-display text-2xl font-medium text-ink">My Enrollments</h1>
      <p className="mt-1 text-sm text-slate-dim">
        Subjects you're currently enrolled in
        {user?.email ? ` · ${user.email}` : ''}
      </p>

      {/* Dashboard cards */}
      <StampGrid className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard icon={ClipboardCheck} label="Enrolled Subjects" value={stats.total} accent={STAT_SHADES[0]} />
        <StatCard icon={BookOpen} label="Courses" value={stats.uniqueCourses} accent={STAT_SHADES[2]} />
        <StatCard icon={GraduationCap} label="Teachers" value={stats.uniqueTeachers} accent={STAT_SHADES[4]} />
      </StampGrid>

      {/* Search */}
      <div className="relative mt-6">
        <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-dim" />
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by subject, course or teacher..."
          className="w-full rounded-lg border border-parchment-line bg-white/60 py-2 pl-9 pr-3 text-sm text-ink placeholder:text-slate-dim focus:border-brass focus:outline-none sm:max-w-md"
        />
      </div>

      {/* Results */}
      <div className="paper mt-6 rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)]">
        <PanelHeader
          icon={Layers}
          title="Enrolled Subjects"
          note={!error && filtered.length > 0 ? `${filtered.length} of ${enrollments.length} shown` : undefined}
        />

        {loading ? (
          <p className="py-6 text-center text-sm text-slate-dim">Loading your enrollments...</p>
        ) : error ? (
          <PanelError message={error} />
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-2 py-10 text-center">
            <Layers size={32} className="text-slate-dim/50" />
            <p className="font-display text-base font-medium text-ink">
              {search ? 'No subjects match your search' : 'No enrollments yet'}
            </p>
            <p className="text-sm text-slate-dim">
              {search
                ? 'Try a different search term.'
                : "You aren't enrolled in any subjects yet. Contact your administrator if this seems wrong."}
            </p>
          </div>
        ) : (
          <>
            {/* Table — visible from sm up, where columns have room to breathe */}
            <div className="hidden overflow-x-auto sm:block">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-parchment-line text-slate-dim">
                    <th className="px-3 py-3 font-medium">Subject</th>
                    <th className="px-3 py-3 font-medium">Course</th>
                    <th className="px-3 py-3 font-medium">Teacher</th>
                    <th className="px-3 py-3 font-medium">Semester</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((en) => (
                    <tr key={en.id} className="border-b border-parchment-line last:border-0 hover:bg-white/80">
                      <td className="px-3 py-3 text-ink">{en.subjectName}</td>
                      <td className="px-3 py-3 text-slate-dim">{en.courseName}</td>
                      <td className="px-3 py-3 text-slate-dim">{en.teacherName}</td>
                      <td className="px-3 py-3 text-slate-dim">{en.semester}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Card list — mobile only, replaces the table instead of forcing
                horizontal scroll on a narrow screen. */}
            <ul className="space-y-3 sm:hidden">
              {filtered.map((en) => (
                <li key={en.id} className="rounded-lg border border-parchment-line p-4">
                  <div className="flex items-start justify-between gap-3">
                    <p className="font-display text-sm font-medium text-ink">{en.subjectName}</p>
                    <span className="shrink-0 rounded bg-parchment px-2 py-0.5 font-mono text-xs text-slate">
                      Sem {en.semester}
                    </span>
                  </div>
                  <p className="mt-1 text-xs text-slate-dim">{en.courseName}</p>
                  <p className="mt-2 flex items-center gap-1 text-xs text-slate-dim">
                    <GraduationCap size={12} />
                    {en.teacherName}
                  </p>
                </li>
              ))}
            </ul>
          </>
        )}
      </div>
    </Layout>
  )
}
