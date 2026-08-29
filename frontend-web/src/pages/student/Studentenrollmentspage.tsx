// src/pages/student/Studentenrollmentspage.tsx
import { useEffect, useMemo, useState } from 'react'
import { Layout } from '@/components/Layout'
import { StampGrid } from '@/components/motion'
import { StatCard, PanelHeader, PanelError, STAT_SHADES } from '@/components/PageBits'
import { useAuthStore } from '@/store/authStore'
import { ClipboardCheck, Search, Layers, GraduationCap, BookOpen } from 'lucide-react'
import { getMyClassEnrollments, getMyClassesAsStudent, getClassSubjectsForStudent, selfEnrollInClassSubject, type ClassEnrollment, type ClassSubject } from '@/api/schoolClass'

export function StudentEnrollmentsPage() {
  const { user } = useAuthStore()
  const [enrollments, setEnrollments] = useState<ClassEnrollment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [available, setAvailable] = useState<ClassSubject[]>([])
  const [enrollingId, setEnrollingId] = useState<number | null>(null)

  useEffect(() => {
    let mounted = true
    async function load() {
      setLoading(true)
      setError(null)
      try {
        // ClassEnrollment is the single source of truth for the student's
        // current class/subject participation.
        const [classEnrollments, classes] = await Promise.all([
          getMyClassEnrollments(),
          getMyClassesAsStudent(),
        ])
        const subjectLists = await Promise.all(classes.map((c) => getClassSubjectsForStudent(c.id)))
        const availableSubjects = subjectLists.flat().filter((s) => !s.enrolledByMe)
        if (mounted) {
          setEnrollments(classEnrollments)
          setAvailable(availableSubjects)
        }
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
        (e.className ?? '').toLowerCase().includes(q) ||
        (e.teacherName ?? '').toLowerCase().includes(q),
    )
  }, [enrollments, search])

  const stats = useMemo(() => {
    const uniqueCourses = new Set(enrollments.map((e) => e.className).filter(Boolean)).size
    const uniqueTeachers = new Set(enrollments.map((e) => e.teacherName)).size
    return { total: enrollments.length, uniqueCourses, uniqueTeachers }
  }, [enrollments])

  return (
    <Layout>
      <h1 className="font-heading text-2xl font-medium text-text">My Enrollments</h1>
      <p className="mt-1 text-sm text-muted">
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
        <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by subject, course or teacher..."
          className="w-full rounded-lg border border-border bg-white/60 py-2 pl-9 pr-3 text-sm text-text placeholder:text-muted focus:border-primary focus:outline-none sm:max-w-md"
        />
      </div>

      {/* Available subjects */}
      {!loading && !error && available.length > 0 && (
        <div className="leaf-card mt-6 rounded-lg border border-border bg-white/60 p-5 shadow-[var(--shadow-card-hover)]">
          <PanelHeader icon={BookOpen} title="Available Subjects" note="Elective subjects you can join" />
          <div className="mt-4 grid gap-3 md:grid-cols-2">
            {available.map((subject) => (
              <div key={subject.id} className="rounded-lg border border-border p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-heading text-sm font-medium text-text">{subject.subjectName}</p>
                    <p className="mt-1 text-xs text-muted">{subject.subjectCode} · {subject.teacherName}</p>
                  </div>
                  <button
                    type="button"
                    disabled={enrollingId === subject.id}
                    onClick={async () => {
                      setEnrollingId(subject.id)
                      try {
                        const enrollment = await selfEnrollInClassSubject(subject.id)
                        setEnrollments((current) => [...current, enrollment])
                        setAvailable((current) => current.filter((s) => s.id !== subject.id))
                      } catch {
                        setError('Failed to enroll in the subject. Please try again.')
                      } finally {
                        setEnrollingId(null)
                      }
                    }}
                    className="shrink-0 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-white disabled:opacity-50"
                  >
                    {enrollingId === subject.id ? 'Joining…' : 'Enroll'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Results */}
      <div className="leaf-card mt-6 rounded-lg border border-border bg-white/60 p-5 shadow-[var(--shadow-card-hover)]">
        <PanelHeader
          icon={Layers}
          title="Enrolled Subjects"
          note={!error && filtered.length > 0 ? `${filtered.length} of ${enrollments.length} shown` : undefined}
        />

        {loading ? (
          <p className="py-6 text-center text-sm text-muted">Loading your enrollments...</p>
        ) : error ? (
          <PanelError message={error} />
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-2 py-10 text-center">
            <Layers size={32} className="text-muted/50" />
            <p className="font-heading text-base font-medium text-text">
              {search ? 'No subjects match your search' : 'No enrollments yet'}
            </p>
            <p className="text-sm text-muted">
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
                  <tr className="border-b border-border text-muted">
                    <th className="px-3 py-3 font-medium">Subject</th>
                    <th className="px-3 py-3 font-medium">Course</th>
                    <th className="px-3 py-3 font-medium">Teacher</th>
                    <th className="px-3 py-3 font-medium">Semester</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((en) => (
                    <tr key={en.id} className="border-b border-border last:border-0 hover:bg-white/80">
                      <td className="px-3 py-3 text-text">{en.subjectName}</td>
                      <td className="px-3 py-3 text-muted">{en.className ?? '—'}</td>
                      <td className="px-3 py-3 text-muted">{en.teacherName ?? '—'}</td>
                      <td className="px-3 py-3 text-muted">{en.semester ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Card list — mobile only, replaces the table instead of forcing
                horizontal scroll on a narrow screen. */}
            <ul className="space-y-3 sm:hidden">
              {filtered.map((en) => (
                <li key={en.id} className="rounded-lg border border-border p-4">
                  <div className="flex items-start justify-between gap-3">
                    <p className="font-heading text-sm font-medium text-text">{en.subjectName}</p>
                    <span className="shrink-0 rounded bg-bg px-2 py-0.5 font-numbers text-xs text-muted">
                      Sem {en.semester}
                    </span>
                  </div>
                  <p className="mt-1 text-xs text-muted">{en.className ?? '—'}</p>
                  <p className="mt-2 flex items-center gap-1 text-xs text-muted">
                    <GraduationCap size={12} />
                    {en.teacherName ?? '—'}
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
