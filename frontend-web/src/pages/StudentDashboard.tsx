// src/pages/StudentDashboard.tsx
import { useEffect, useMemo, useState } from 'react'
import { motion } from 'framer-motion'
import { Layout } from '@/components/Layout'
import { StampGrid, StampItem, TallyCounter, LedgerRule } from '@/components/motion'
import {
  getMyProfile,
  getStudentDashboardSummary,
  getMyAssignments,
  getMyResults,
  getMyTimetable,
  getMyNotifications,
  type StudentProfile,
} from '@/api'
import { getMyAttendanceSummary } from '@/api/attendance'
import {
  CalendarCheck2,
  BookOpen,
  ClipboardList,
  ListTodo,
  Award,
  Clock,
  AlarmClockCheck,
  Megaphone,
  Loader2,
  AlertTriangle,
} from 'lucide-react'
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  PieChart,
  Pie,
  Cell,
} from 'recharts'

/* ------------------------------------------------------------------ */
/* View-model types                                                    */
/* ------------------------------------------------------------------ */

interface SubjectMark {
  subject: string
  marks: number
}

interface SubjectAttendancePoint {
  subject: string
  percentage: number
}

interface TodayClass {
  id: string
  subject: string
  time: string
  teacher: string | null
  room: string | null
}

interface Deadline {
  id: number
  title: string
  subject: string
  dueDate: string
}

interface Announcement {
  id: number
  title: string
  body: string
  postedAt: string
}

interface DashboardData {
  course: string | null
  department: string | null
  semester: number | null
  attendancePercentage: number
  subjectsCount: number
  assignmentsCount: number
  pendingWorkCount: number
  averageMarks: number
  attendanceBySubject: SubjectAttendancePoint[]
  subjectMarks: SubjectMark[]
  todayClasses: TodayClass[]
  upcomingDeadlines: Deadline[]
  announcements: Announcement[]
  timetableIsPlaceholder: boolean
  failed: {
    summary: boolean
    attendance: boolean
    assignments: boolean
    results: boolean
    timetable: boolean
    notifications: boolean
  }
}

const COLORS = {
  primary: '#2e7d32',
  secondary: '#4caf50',
  text: '#1f2937',
  danger: '#c1543c',
  muted: '#6b7280',
  green: '#3f7d55',
  grid: '#eef2e7',
}

const STAT_SHADES = [
  '#1b5e20',
  '#2e7d32',
  '#388e3c',
  '#43a047',
  '#4caf50',
  '#66bb6a',
  '#81c784',
  '#2e7d32',
]

const PIE_COLORS = [COLORS.primary, '#e8f5e9']
const EASE_STAMP = [0.16, 1, 0.3, 1] as const

const panelIn = {
  hidden: { opacity: 0, y: 14 },
  show: (i: number) => ({
    opacity: 1,
    y: 0,
    transition: { duration: 0.4, ease: EASE_STAMP, delay: 0.06 * i },
  }),
}

const listStagger = {
  hidden: {},
  show: { transition: { staggerChildren: 0.055, delayChildren: 0.05 } },
}

const listItem = {
  hidden: { opacity: 0, x: -8 },
  show: { opacity: 1, x: 0, transition: { duration: 0.26, ease: EASE_STAMP } },
}

function StatCard({
  icon: Icon,
  label,
  value,
  suffix,
  accent,
  failed,
}: {
  icon: typeof CalendarCheck2
  label: string
  value: string | number
  suffix?: string
  accent?: string
  failed?: boolean
}) {
  return (
    <StampItem className="relative px-5 pt-5 pb-6">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted">{label}</p>
        <span
          className="flex h-9 w-9 items-center justify-center rounded-xl"
          style={{
            backgroundColor: (failed ? COLORS.danger : accent ?? COLORS.primary) + '1f',
            color: failed ? COLORS.danger : accent ?? COLORS.primary,
          }}
        >
          {failed ? <AlertTriangle size={18} /> : <Icon size={18} />}
        </span>
      </div>
      {failed ? (
        <p className="mt-3 text-sm font-medium text-danger">Couldn't load</p>
      ) : (
        <p className="mt-3 font-numbers text-3xl font-bold text-text">
          {typeof value === 'number' ? <TallyCounter value={value} /> : value}
          {suffix && <span className="ml-1 text-lg font-medium text-muted">{suffix}</span>}
        </p>
      )}
    </StampItem>
  )
}

function PanelHeader({ icon: Icon, title, note }: { icon: typeof CalendarCheck2; title: string; note?: string }) {
  return (
    <div className="mb-4 pb-3">
      <div className="flex items-center gap-2">
        <Icon size={16} className="text-primary" />
        <h3 className="font-heading text-base font-medium text-text">{title}</h3>
      </div>
      {note && <p className="mt-1 text-xs italic text-muted">{note}</p>}
      <LedgerRule className="mt-3" />
    </div>
  )
}

function PanelError({ message = "Couldn't load this section. Try refreshing the page." }: { message?: string }) {
  return (
    <div className="flex items-start gap-2 text-sm text-danger">
      <AlertTriangle size={16} className="mt-0.5 shrink-0" />
      <p>{message}</p>
    </div>
  )
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString(undefined, {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  })
}

function daysUntil(iso: string) {
  const diff = Math.ceil((new Date(iso).getTime() - Date.now()) / (1000 * 60 * 60 * 24))
  if (diff < 0) return 'Overdue'
  if (diff === 0) return 'Due today'
  if (diff === 1) return 'Due tomorrow'
  return `Due in ${diff} days`
}

function todayScheduleKey() {
  return new Date().toLocaleDateString('en-US', { weekday: 'long' }).toUpperCase()
}

export function StudentDashboard() {
  const [profile, setProfile] = useState<StudentProfile | null>(null)
  const [data, setData] = useState<DashboardData | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function load() {
      const [
        profileResult,
        summaryResult,
        attendanceResult,
        assignmentsResult,
        resultsResult,
        timetableResult,
        notificationsResult,
      ] = await Promise.allSettled([
        getMyProfile(),
        getStudentDashboardSummary(),
        getMyAttendanceSummary(),
        getMyAssignments(),
        getMyResults(),
        getMyTimetable(),
        getMyNotifications(),
      ])

      if (cancelled) return

      if (profileResult.status === 'fulfilled') setProfile(profileResult.value)

      const summary = summaryResult.status === 'fulfilled' ? summaryResult.value : null
      const attendance = attendanceResult.status === 'fulfilled' ? attendanceResult.value : null
      const assignments = assignmentsResult.status === 'fulfilled' ? assignmentsResult.value : []
      const results = resultsResult.status === 'fulfilled' ? resultsResult.value : null
      const timetable = timetableResult.status === 'fulfilled' ? timetableResult.value : null
      const notifications = notificationsResult.status === 'fulfilled' ? notificationsResult.value : []

      const failed = {
        summary: summaryResult.status === 'rejected',
        attendance: attendanceResult.status === 'rejected',
        assignments: assignmentsResult.status === 'rejected',
        results: resultsResult.status === 'rejected',
        timetable: timetableResult.status === 'rejected',
        notifications: notificationsResult.status === 'rejected',
      }

      const latestSemester = results?.semesterResults?.length
        ? results.semesterResults[results.semesterResults.length - 1]
        : null

      const subjectMarks: SubjectMark[] = (latestSemester?.subjects ?? []).map((s) => ({
        subject: s.subjectName,
        marks: s.maxMarks > 0 ? Math.round((s.totalMarks / s.maxMarks) * 100) : 0,
      }))

      const averageMarks = subjectMarks.length
        ? Math.round(subjectMarks.reduce((sum, s) => sum + s.marks, 0) / subjectMarks.length)
        : 0

      const attendanceBySubject: SubjectAttendancePoint[] = (attendance?.bySubject ?? []).map((s) => ({
        subject: s.subjectName,
        percentage: Math.round(s.attendancePercentage),
      }))

      const notSubmitted = assignments.filter((a) => a.submissionStatus === 'NOT_SUBMITTED')

      const upcomingDeadlines: Deadline[] = notSubmitted
        .slice()
        .sort((a, b) => new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime())
        .slice(0, 5)
        .map((a) => ({
          id: a.assignmentId,
          title: a.title,
          subject: a.subjectName,
          dueDate: a.dueDate,
        }))

      const todaysEntries = timetable?.schedule?.[todayScheduleKey()] ?? []
      const todayClasses: TodayClass[] = todaysEntries.map((entry, index) => ({
        id: `${entry.classSubjectId ?? entry.subjectId ?? 'class'}-${index}`,
        subject: entry.subjectName,
        time: `${entry.startTime} - ${entry.endTime}`,
        teacher: entry.teacherName ?? null,
        room: entry.room ?? null,
      }))

      const announcements: Announcement[] = notifications.map((n) => ({
        id: n.id,
        title: n.title,
        body: n.message,
        postedAt: n.createdAt,
      }))

      setData({
        course: summary?.course ?? null,
        department: summary?.department ?? null,
        semester: summary?.semester ?? null,
        attendancePercentage: Math.round(attendance?.overallAttendancePercentage ?? summary?.attendancePercentage ?? 0),
        subjectsCount: summary?.totalSubjects ?? attendance?.bySubject.length ?? 0,
        assignmentsCount: assignments.length,
        pendingWorkCount: notSubmitted.length,
        averageMarks,
        attendanceBySubject,
        subjectMarks,
        todayClasses,
        upcomingDeadlines,
        announcements,
        timetableIsPlaceholder: timetable?.placeholder ?? true,
        failed,
      })
      setLoading(false)
    }

    load()
    return () => {
      cancelled = true
    }
  }, [])

  const pieData = useMemo(() => {
    if (!data) return []
    return [
      { name: 'Present', value: data.attendancePercentage },
      { name: 'Absent', value: 100 - data.attendancePercentage },
    ]
  }, [data])

  if (loading || !data) {
    return (
      <Layout>
        <div className="flex h-64 items-center justify-center gap-2 text-muted">
          <Loader2 size={18} className="animate-spin" />
          Loading dashboard…
        </div>
      </Layout>
    )
  }

  const photoUrl = (profile as (StudentProfile & { photoUrl?: string; avatarUrl?: string }) | null)?.photoUrl
    ?? (profile as (StudentProfile & { photoUrl?: string; avatarUrl?: string }) | null)?.avatarUrl
  const initials =
    profile?.firstName || profile?.lastName
      ? `${profile?.firstName?.[0] ?? ''}${profile?.lastName?.[0] ?? ''}`.toUpperCase()
      : (profile?.email?.[0] ?? '?').toUpperCase()

  const attendanceFailed = data.failed.attendance && data.failed.summary

  return (
    <Layout>
      <div className="relative mb-6">
        <h1 className="font-heading text-2xl font-medium text-text">
          Welcome{profile?.firstName ? `, ${profile.firstName}` : ''}
        </h1>
        <p className="mt-1 text-sm text-muted">Here's what's happening with your studies.</p>
        {(data.course || data.department || data.semester) && (
          <p className="mt-1 text-sm text-muted">
            {[data.course, data.department, data.semester ? `Semester ${data.semester}` : null]
              .filter(Boolean)
              .join(' · ')}
          </p>
        )}

        <div className="sprout-in absolute -top-1 right-0 hidden sm:block">
          {photoUrl ? (
            <img
              src={photoUrl}
              alt="Your profile photo"
              className="h-14 w-14 rounded-full border-2 border-white object-cover shadow-[var(--shadow-card-hover)]"
            />
          ) : (
            <div
              className="flex h-14 w-14 items-center justify-center rounded-full border-2 border-white font-heading text-lg font-medium text-white shadow-[var(--shadow-card-hover)]"
              style={{ backgroundColor: COLORS.primary }}
            >
              {initials}
            </div>
          )}
        </div>
      </div>

      <StampGrid className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          icon={CalendarCheck2}
          label="Attendance"
          value={data.attendancePercentage}
          suffix="%"
          accent={STAT_SHADES[0]}
          failed={attendanceFailed}
        />
        <StatCard
          icon={BookOpen}
          label="Subjects"
          value={data.subjectsCount}
          accent={STAT_SHADES[1]}
          failed={data.failed.summary}
        />
        <StatCard
          icon={ClipboardList}
          label="Assignments"
          value={data.assignmentsCount}
          accent={STAT_SHADES[2]}
          failed={data.failed.assignments}
        />
        <StatCard
          icon={ListTodo}
          label="Pending work"
          value={data.pendingWorkCount}
          accent={STAT_SHADES[3]}
          failed={data.failed.assignments}
        />
        <StatCard
          icon={Award}
          label="Average marks"
          value={data.averageMarks}
          suffix="%"
          accent={STAT_SHADES[4]}
          failed={data.failed.results}
        />
      </StampGrid>

      <div className="grid gap-6 lg:grid-cols-2">
        <motion.section
          custom={0}
          variants={panelIn}
          initial="hidden"
          animate="show"
          className="rounded-2xl bg-white p-5 shadow-[var(--shadow-card)]"
        >
          <PanelHeader icon={CalendarCheck2} title="Attendance by subject" />
          {attendanceFailed ? (
            <PanelError />
          ) : data.attendanceBySubject.length ? (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={data.attendanceBySubject} margin={{ top: 8, right: 12, left: 0, bottom: 8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke={COLORS.grid} />
                <XAxis dataKey="subject" tick={{ fontSize: 11 }} />
                <YAxis domain={[0, 100]} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="percentage" fill={COLORS.primary} radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <p className="text-sm text-muted">No attendance records yet.</p>
          )}
        </motion.section>

        <motion.section
          custom={1}
          variants={panelIn}
          initial="hidden"
          animate="show"
          className="rounded-2xl bg-white p-5 shadow-[var(--shadow-card)]"
        >
          <PanelHeader icon={Award} title="Marks overview" />
          {data.failed.results ? (
            <PanelError />
          ) : data.subjectMarks.length ? (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={data.subjectMarks} margin={{ top: 8, right: 12, left: 0, bottom: 8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke={COLORS.grid} />
                <XAxis dataKey="subject" tick={{ fontSize: 11 }} />
                <YAxis domain={[0, 100]} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="marks" fill={COLORS.secondary} radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <p className="text-sm text-muted">No published marks yet.</p>
          )}
        </motion.section>

        <motion.section
          custom={2}
          variants={panelIn}
          initial="hidden"
          animate="show"
          className="rounded-2xl bg-white p-5 shadow-[var(--shadow-card)]"
        >
          <PanelHeader icon={Clock} title="Today's timetable" />
          {data.failed.timetable ? (
            <PanelError />
          ) : data.todayClasses.length ? (
            <motion.div variants={listStagger} initial="hidden" animate="show" className="space-y-3">
              {data.todayClasses.map((item) => (
                <motion.div key={item.id} variants={listItem} className="flex items-center justify-between rounded-xl border border-slate-100 px-4 py-3">
                  <div>
                    <p className="font-medium text-text">{item.subject}</p>
                    <p className="mt-1 text-xs text-muted">
                      {item.teacher ?? 'Teacher not assigned'}{item.room ? ` · Room ${item.room}` : ''}
                    </p>
                  </div>
                  <span className="text-sm font-medium text-primary">{item.time}</span>
                </motion.div>
              ))}
            </motion.div>
          ) : (
            <p className="text-sm text-muted">No classes scheduled today.</p>
          )}
        </motion.section>

        <motion.section
          custom={3}
          variants={panelIn}
          initial="hidden"
          animate="show"
          className="rounded-2xl bg-white p-5 shadow-[var(--shadow-card)]"
        >
          <PanelHeader icon={AlarmClockCheck} title="Upcoming deadlines" />
          {data.failed.assignments ? (
            <PanelError />
          ) : data.upcomingDeadlines.length ? (
            <motion.div variants={listStagger} initial="hidden" animate="show" className="space-y-3">
              {data.upcomingDeadlines.map((item) => (
                <motion.div key={item.id} variants={listItem} className="flex items-center justify-between rounded-xl border border-slate-100 px-4 py-3">
                  <div>
                    <p className="font-medium text-text">{item.title}</p>
                    <p className="mt-1 text-xs text-muted">{item.subject}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs font-medium text-primary">{daysUntil(item.dueDate)}</p>
                    <p className="mt-1 text-[11px] text-muted">{formatDate(item.dueDate)}</p>
                  </div>
                </motion.div>
              ))}
            </motion.div>
          ) : (
            <p className="text-sm text-muted">No pending assignment deadlines.</p>
          )}
        </motion.section>

        <motion.section
          custom={4}
          variants={panelIn}
          initial="hidden"
          animate="show"
          className="rounded-2xl bg-white p-5 shadow-[var(--shadow-card)]"
        >
          <PanelHeader icon={Megaphone} title="Announcements" />
          {data.failed.notifications ? (
            <PanelError />
          ) : data.announcements.length ? (
            <motion.div variants={listStagger} initial="hidden" animate="show" className="space-y-3">
              {data.announcements.slice(0, 5).map((item) => (
                <motion.div key={item.id} variants={listItem} className="rounded-xl border border-slate-100 px-4 py-3">
                  <div className="flex items-start justify-between gap-3">
                    <p className="font-medium text-text">{item.title}</p>
                    <span className="shrink-0 text-[11px] text-muted">{formatDate(item.postedAt)}</span>
                  </div>
                  <p className="mt-1 text-sm text-muted">{item.body}</p>
                </motion.div>
              ))}
            </motion.div>
          ) : (
            <p className="text-sm text-muted">No announcements yet.</p>
          )}
        </motion.section>

        <motion.section
          custom={5}
          variants={panelIn}
          initial="hidden"
          animate="show"
          className="rounded-2xl bg-white p-5 shadow-[var(--shadow-card)]"
        >
          <PanelHeader icon={CalendarCheck2} title="Attendance snapshot" note="Overall attendance for your enrolled class subjects" />
          {attendanceFailed ? (
            <PanelError />
          ) : (
            <div className="flex items-center justify-center">
              <div className="relative h-56 w-56">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie data={pieData} dataKey="value" nameKey="name" innerRadius={70} outerRadius={92} paddingAngle={2} startAngle={90} endAngle={-270}>
                      {pieData.map((_, index) => (
                        <Cell key={`attendance-${index}`} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
                <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
                  <span className="font-numbers text-3xl font-bold text-text">{data.attendancePercentage}%</span>
                  <span className="text-xs text-muted">attendance</span>
                </div>
              </div>
            </div>
          )}
        </motion.section>
      </div>
    </Layout>
  )
}
