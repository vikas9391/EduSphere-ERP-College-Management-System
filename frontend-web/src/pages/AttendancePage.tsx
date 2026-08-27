// src/pages/AttendancePage.tsx
import { useEffect, useMemo, useState } from 'react'
import { Layout } from '@/components/Layout'
import { Field, inputClass } from '@/components/FormField'
import { StampGrid } from '@/components/motion'
import { StatCard, PanelError, STAT_SHADES } from '@/components/PageBits'
import {
  CalendarDays,
  Search,
  Filter,
  Check,
  X,
  Pencil,
  Trash2,
  Users,
  UserCheck,
  UserX,
  Percent,
  Save,
} from 'lucide-react'
import {
  getAttendance,
  createAttendance,
  deleteAttendance,
  updateAttendance,
  type Attendance,
  type AttendancePayload,
} from '@/api/attendance'
import { getEnrollments } from '@/api'
import { getMySubjects, type Subject } from '@/api'
import { getMyClasses, getClassSubjects, getClassSubjectEnrollments, type ClassSubject } from '@/api/schoolClass'

type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED'

interface RosterRow {
  studentId: number
  enrollmentId: number | null
  classEnrollmentId: number | null
  studentName: string
  admissionNo: string
  status: AttendanceStatus
  existingRecordId: number | null
}

function todayISO() {
  return new Date().toISOString().slice(0, 10)
}

export function AttendancePage() {
  const [records, setRecords] = useState<Attendance[]>([])
  const [subjects, setSubjects] = useState<Subject[]>([])
  const [classSubjects, setClassSubjects] = useState<ClassSubject[]>([])
  const [classSubjectEnrollments, setClassSubjectEnrollments] = useState<Record<number, { id:number; studentId:number; studentName:string }[]>>({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // History filters
  const [search, setSearch] = useState('')
  const [subjectFilter, setSubjectFilter] = useState('')
  const [dateFilter, setDateFilter] = useState('')

  // Mark attendance panel
  const [markSubjectId, setMarkSubjectId] = useState('')
  const [markDate, setMarkDate] = useState(todayISO())
  const [roster, setRoster] = useState<RosterRow[]>([])
  const [rosterLoading, setRosterLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saveSuccess, setSaveSuccess] = useState(false)

  async function loadAll() {
    setLoading(true)
    setError(null)
    try {
      const [attendanceResult, subjectsResult, classesResult] = await Promise.allSettled([
        getAttendance(),
        getMySubjects(),
        getMyClasses(),
      ])

      if (attendanceResult.status === 'fulfilled') setRecords(attendanceResult.value)
      else {
        setRecords([])
        setError('Failed to load attendance records. Please try again.')
      }

      if (subjectsResult.status === 'fulfilled') setSubjects(subjectsResult.value)
      else setSubjects([])

      if (classesResult.status === 'fulfilled') {
        const allClassSubjects: ClassSubject[] = []
        const enrollmentMap: Record<number, { id:number; studentId:number; studentName:string }[]> = {}
        for (const cls of classesResult.value) {
          try {
            const items = await getClassSubjects(cls.id)
            allClassSubjects.push(...items)
            for (const item of items) {
              try {
                const enrolled = await getClassSubjectEnrollments(item.id)
                enrollmentMap[item.id] = enrolled.map(e => ({
                  id: e.id, studentId: e.studentId, studentName: e.studentName
                }))
              } catch {}
            }
          } catch {}
        }
        setClassSubjects(allClassSubjects)
        setClassSubjectEnrollments(enrollmentMap)
      }
    } catch {
      setError('Failed to load attendance data. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAll()
  }, [])

  useEffect(() => {
    if (!markSubjectId || !markDate) {
      setRoster([])
      return
    }
    let mounted = true
    async function loadRoster() {
      setRosterLoading(true)
      setSaveError(null)
      setSaveSuccess(false)
      try {
        const selectedClassSubject = classSubjects.find(s => String(s.id) === markSubjectId)
        const existingForDate = records.filter(r =>
          (selectedClassSubject ? r.subjectName === selectedClassSubject.subjectName : String(r.subjectId) === markSubjectId)
          && r.attendanceDate.slice(0, 10) === markDate
        )
        const classEnrolled = selectedClassSubject ? (classSubjectEnrollments[selectedClassSubject.id] ?? []) : []
        const enrolled = selectedClassSubject
          ? classEnrolled.map(e => ({ id:e.id, studentId:e.studentId, studentName:e.studentName, admissionNo:'' }))
          : (await getEnrollments()).filter(e => String(e.subjectId) === markSubjectId)
        const rows: RosterRow[] = enrolled.map(e => {
          const existing = existingForDate.find(r => r.studentId === e.studentId)
          return {
            studentId: e.studentId,
            enrollmentId: selectedClassSubject ? null : e.id,
            classEnrollmentId: selectedClassSubject ? e.id : null,
            studentName: e.studentName,
            admissionNo: e.admissionNo,
            status: (existing?.status as AttendanceStatus) ?? 'PRESENT',
            existingRecordId: existing?.id ?? null,
          }
        })
        if (mounted) setRoster(rows)
      } catch {
        if (mounted) setSaveError('Failed to load the class roster for this subject.')
      } finally {
        if (mounted) setRosterLoading(false)
      }
    }
    loadRoster()
    return () => { mounted = false }
  }, [markSubjectId, markDate, records, classSubjects, classSubjectEnrollments])

  function setRowStatus(studentId: number, status: AttendanceStatus) {
    setRoster((prev) => prev.map((r) => (r.studentId === studentId ? { ...r, status } : r)))
  }

  function markAllAs(status: AttendanceStatus) {
    setRoster((prev) => prev.map((r) => ({ ...r, status })))
  }

  async function handleSaveAttendance() {
    if (!markSubjectId || !markDate || roster.length === 0) return
    setSaving(true)
    setSaveError(null)
    setSaveSuccess(false)
    try {
      await Promise.all(
        roster.map(async (r) => {
          const payload: AttendancePayload = {
            enrollmentId: r.enrollmentId,
            classEnrollmentId: r.classEnrollmentId,
            attendanceDate: markDate,
            status: r.status,
            remarks: '',
          }
          if (r.existingRecordId) return updateAttendance(r.existingRecordId, payload)
          return createAttendance(payload)
        }),
      )
      await loadAll()
      setSaveSuccess(true)
    } catch {
      setSaveError('Failed to save attendance. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  const courseNameForFilter = useMemo(() => {
    const m = new Map<number, string>()
    subjects.forEach((s) => m.set(s.id, s.courseName))
    return m
  }, [subjects])

  const filtered = useMemo(() => {
    return records.filter((r) => {
      const matchesSearch =
        !search ||
        r.studentName.toLowerCase().includes(search.toLowerCase()) ||
        r.subjectName.toLowerCase().includes(search.toLowerCase())
      const matchesSubject = !subjectFilter || String(r.subjectId) === subjectFilter
      const matchesDate = !dateFilter || r.attendanceDate.slice(0, 10) === dateFilter
      return matchesSearch && matchesSubject && matchesDate
    })
  }, [records, search, subjectFilter, dateFilter])

  const stats = useMemo(() => {
    const today = todayISO()
    const todays = records.filter((r) => r.attendanceDate.slice(0, 10) === today)
    const presentToday = todays.filter((r) => r.status === 'PRESENT').length
    const absentToday = todays.filter((r) => r.status === 'ABSENT').length
    const overallPresent = records.filter((r) => r.status === 'PRESENT').length
    const rate = records.length ? Math.round((overallPresent / records.length) * 100) : 0
    return { total: records.length, presentToday, absentToday, rate }
  }, [records])

  const hasFilters = !!search || !!subjectFilter || !!dateFilter

  async function handleDelete(id: number) {
    if (!window.confirm('Delete this attendance record? This cannot be undone.')) return
    try {
      await deleteAttendance(id)
      setRecords((prev) => prev.filter((r) => r.id !== id))
    } catch {
      window.alert('Failed to delete the record. Please try again.')
    }
  }

  async function handleToggleStatus(r: Attendance) {
    const nextStatus: AttendanceStatus = r.status === 'PRESENT' ? 'ABSENT' : 'PRESENT'
    try {
      const updated = await updateAttendance(r.id, {
        enrollmentId: r.enrollmentId,
        attendanceDate: r.attendanceDate,
        status: nextStatus,
        remarks: r.remarks,
      })
      setRecords((prev) => prev.map((rec) => (rec.id === r.id ? updated : rec)))
    } catch {
      window.alert('Failed to update attendance status.')
    }
  }

  return (
    <Layout>
      <h1 className="font-heading text-2xl font-medium text-text">Attendance</h1>
      <p className="mt-1 text-sm text-muted">Mark and review student attendance</p>

      {/* Dashboard cards */}
      <StampGrid className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard
          icon={CalendarDays}
          label="Total Records"
          value={stats.total}
          accent={STAT_SHADES[0]}
          failed={!!error}
        />
        <StatCard
          icon={UserCheck}
          label="Present Today"
          value={stats.presentToday}
          accent={STAT_SHADES[1]}
          failed={!!error}
        />
        <StatCard
          icon={UserX}
          label="Absent Today"
          value={stats.absentToday}
          accent={STAT_SHADES[2]}
          failed={!!error}
        />
        <StatCard
          icon={Percent}
          label="Overall Rate"
          value={stats.rate}
          suffix="%"
          accent={STAT_SHADES[3]}
          failed={!!error}
        />
      </StampGrid>

      {/* Mark Attendance panel */}
      <div className="mt-8 rounded-lg border border-border bg-white/60 p-5">
        <h2 className="font-heading text-base font-medium text-text">Mark Attendance</h2>
        <p className="mt-1 text-sm text-muted">Choose a subject and date to load the class roster</p>

        <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 md:max-w-lg">
          <Field label="Subject">
            <select value={markSubjectId} onChange={(e) => setMarkSubjectId(e.target.value)} className={inputClass}>
              <option value="">Select a subject</option>
              {classSubjects.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.subjectName} · {s.subjectCode}
                </option>
              ))}
              {classSubjects.length === 0 && subjects.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.subjectName} · {s.courseName}
                </option>
              ))}
            </select>
          </Field>

          <Field label="Date">
            <input
              type="date"
              value={markDate}
              onChange={(e) => setMarkDate(e.target.value)}
              className={inputClass}
            />
          </Field>
        </div>

        {markSubjectId && markDate && (
          <div className="mt-5">
            {rosterLoading ? (
              <p className="text-sm text-muted">Loading roster...</p>
            ) : roster.length === 0 ? (
              <p className="text-sm text-muted">No students are enrolled in this subject yet.</p>
            ) : (
              <>
                <div className="mb-3 flex items-center justify-between">
                  <p className="text-sm text-muted">{roster.length} student(s) enrolled</p>
                  <div className="flex gap-2">
                    <button
                      onClick={() => markAllAs('PRESENT')}
                      className="inline-flex items-center gap-1 rounded-md border border-border px-2.5 py-1 text-xs text-muted hover:border-primary hover:text-text"
                    >
                      <Check size={12} /> Mark all present
                    </button>
                    <button
                      onClick={() => markAllAs('ABSENT')}
                      className="inline-flex items-center gap-1 rounded-md border border-border px-2.5 py-1 text-xs text-muted hover:border-primary hover:text-text"
                    >
                      <X size={12} /> Mark all absent
                    </button>
                  </div>
                </div>

                <div className="overflow-hidden rounded-lg border border-border">
                  <div className="max-h-80 overflow-y-auto">
                    <table className="w-full text-left text-sm">
                      <thead className="sticky top-0 bg-bg">
                        <tr className="border-b border-border text-muted">
                          <th className="px-4 py-2 font-medium">Student</th>
                          <th className="px-4 py-2 font-medium">Admission No.</th>
                          <th className="px-4 py-2 font-medium text-right">Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {roster.map((r) => (
                          <tr key={r.studentId} className="border-b border-border last:border-0">
                            <td className="px-4 py-2 text-text">{r.studentName}</td>
                            <td className="px-4 py-2 text-muted">{r.admissionNo}</td>
                            <td className="px-4 py-2">
                              <div className="flex justify-end gap-2">
                                <button
                                  onClick={() => setRowStatus(r.studentId, 'PRESENT')}
                                  className={`rounded-md px-3 py-1 text-xs font-medium transition-colors ${
                                    r.status === 'PRESENT'
                                      ? 'bg-green-100 text-green-700'
                                      : 'bg-border/40 text-muted hover:bg-green-50 hover:text-green-700'
                                  }`}
                                >
                                  Present
                                </button>
                                <button
                                  onClick={() => setRowStatus(r.studentId, 'ABSENT')}
                                  className={`rounded-md px-3 py-1 text-xs font-medium transition-colors ${
                                    r.status === 'ABSENT'
                                      ? 'bg-red-100 text-red-700'
                                      : 'bg-border/40 text-muted hover:bg-red-50 hover:text-red-700'
                                  }`}
                                >
                                  Absent
                                </button>
                                <button onClick={() => setRowStatus(r.studentId, 'LATE')} className="rounded-md bg-border/40 px-3 py-1 text-xs font-medium text-muted hover:text-amber-700">Late</button>
                                <button onClick={() => setRowStatus(r.studentId, 'EXCUSED')} className="rounded-md bg-border/40 px-3 py-1 text-xs font-medium text-muted hover:text-blue-700">Excused</button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

                {saveError && <p className="mt-3 text-sm text-red-600">{saveError}</p>}
                {saveSuccess && <p className="mt-3 text-sm text-green-700">Attendance saved successfully.</p>}

                <div className="mt-4 flex justify-end">
                  <button
                    onClick={handleSaveAttendance}
                    disabled={saving}
                    className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary/90 disabled:opacity-60"
                  >
                    <Save size={16} />
                    {saving ? 'Saving...' : 'Save Attendance'}
                  </button>
                </div>
              </>
            )}
          </div>
        )}
      </div>

      {/* History search + filters */}
      <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by student or subject..."
            className="w-full rounded-lg border border-border bg-white/60 py-2 pl-9 pr-3 text-sm text-text placeholder:text-muted focus:border-primary focus:outline-none"
          />
        </div>

        <div className="flex items-center gap-2">
          <Filter size={16} className="text-muted" />
          <select
            value={subjectFilter}
            onChange={(e) => setSubjectFilter(e.target.value)}
            className="rounded-lg border border-border bg-white/60 px-3 py-2 text-sm text-text focus:border-primary focus:outline-none"
          >
            <option value="">All Subjects</option>
            {classSubjects.map((s) => (
              <option key={s.id} value={s.id}>{s.subjectName}</option>
            ))}
            {classSubjects.length === 0 && subjects.map((s) => (
              <option key={s.id} value={s.id}>{s.subjectName}</option>
            ))}
          </select>

          <input
            type="date"
            value={dateFilter}
            onChange={(e) => setDateFilter(e.target.value)}
            className="rounded-lg border border-border bg-white/60 px-3 py-2 text-sm text-text focus:border-primary focus:outline-none"
          />

          {hasFilters && (
            <button
              onClick={() => {
                setSearch('')
                setSubjectFilter('')
                setDateFilter('')
              }}
              className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-2 text-sm text-muted hover:border-primary hover:text-text"
            >
              <X size={14} />
              Clear
            </button>
          )}
        </div>
      </div>

      {/* History table */}
      <div className="mt-6 overflow-hidden rounded-lg border border-border bg-white/60">
        {loading ? (
          <div className="p-10 text-center text-sm text-muted">Loading attendance...</div>
        ) : error ? (
          <div className="p-10">
            <PanelError message={error} />
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-2 p-12 text-center">
            <Users size={32} className="text-muted/50" />
            <p className="font-heading text-base font-medium text-text">
              {hasFilters ? 'No records match your filters' : 'No attendance records yet'}
            </p>
            <p className="text-sm text-muted">
              {hasFilters
                ? 'Try adjusting your search or filters.'
                : 'Mark attendance above to get started.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-200 text-left text-sm">
              <thead>
                <tr className="border-b border-border text-muted">
                  <th className="px-5 py-3 font-medium">Student</th>
                  <th className="px-5 py-3 font-medium">Subject</th>
                  <th className="px-5 py-3 font-medium">Course</th>
                  <th className="px-5 py-3 font-medium">Date</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                  <th className="px-5 py-3 font-medium text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((r) => (
                  <tr key={r.id} className="border-b border-border last:border-0 hover:bg-white/80">
                    <td className="px-5 py-3 text-text">{r.studentName}</td>
                    <td className="px-5 py-3 text-text">{r.subjectName}</td>
                    <td className="px-5 py-3 text-muted">
                      {courseNameForFilter.get(r.subjectId) || '—'}
                    </td>
                    <td className="px-5 py-3 text-muted">{r.attendanceDate.slice(0, 10)}</td>
                    <td className="px-5 py-3">
                      <span
                        className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                          r.status === 'PRESENT' ? 'bg-green-100 text-green-700' : r.status === 'ABSENT' ? 'bg-red-100 text-red-700' : r.status === 'LATE' ? 'bg-amber-100 text-amber-700' : 'bg-blue-100 text-blue-700'
                        }`}
                      >
                        {r.status.charAt(0) + r.status.slice(1).toLowerCase()}
                      </span>
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => handleToggleStatus(r)}
                          className="rounded-md p-1.5 text-muted hover:bg-border/50 hover:text-primary"
                          aria-label="Toggle status"
                        >
                          <Pencil size={16} />
                        </button>
                        <button
                          onClick={() => handleDelete(r.id)}
                          className="rounded-md p-1.5 text-muted hover:bg-red-50 hover:text-red-600"
                          aria-label="Delete record"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </Layout>
  )
}














