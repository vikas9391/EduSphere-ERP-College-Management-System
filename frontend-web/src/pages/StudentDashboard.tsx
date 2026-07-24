// src/pages/StudentDashboard.tsx
import { useEffect, useMemo, useState } from 'react'
import { motion } from 'framer-motion'
import { Layout } from '@/components/Layout'
import { StampGrid, StampItem, TallyCounter, LedgerRule } from '@/components/motion'
import {
  getMyProfile,
  getStudentDashboardSummary,
  getMyAttendanceSummary,
  getMyAssignments,
  getMyResults,
  getMyTimetable,
  getMyNotifications,
  type StudentProfile,
} from '@/api'
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
  teacher: string
  room: string
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
  /** True if the timetable/announcements sections below are backend placeholder data,
   *  not real scheduling/notification data - see api/studentPortal.ts for why. */
  timetableIsPlaceholder: boolean
  /** Per-section fetch failure flags. A failed section is shown with an
   *  explicit "couldn't load" state, distinct from a genuine zero/empty
   *  result, so a student doesn't mistake "the request 404'd" for "you
   *  have 0% attendance". */
  failed: {
    summary: boolean
    attendance: boolean
    assignments: boolean
    results: boolean
    timetable: boolean
    notifications: boolean
  }
}

/* ------------------------------------------------------------------ */
/* Palette (matches the parchment / ink / brass theme)                */
/* ------------------------------------------------------------------ */

const COLORS = {
  brass: '#c9a227',
  brassBright: '#d8b74a',
  ink: '#2b2620',
  brick: '#b5533c',
  slate: '#6b6558',
  slateDim: '#8a8578',
  green: '#3f7d55',
  grid: '#e7ddc9',
}

// One tonal family (shades of brass) instead of mixed accent colors across
// the 8 stat cards — reads as a deliberate, cohesive set rather than a
// traffic-light mix of unrelated hues.
const STAT_SHADES = [
  '#8a6a1c',
  '#a9812f',
  '#c9a227',
  '#b58f26',
  '#d8b74a',
  '#c2a04a',
  '#e2c66e',
  '#cfa93a',
]

const PIE_COLORS = [COLORS.brass, '#efe6cf']

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

/* ------------------------------------------------------------------ */
/* Small presentational helpers                                       */
/* ------------------------------------------------------------------ */

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
  /** When true, shows a "couldn't load" state instead of the value - used
   *  when the backing request failed rather than genuinely returning zero. */
  failed?: boolean
}) {
  return (
    <div className="stat-stack">
      <StampItem className="stat-card relative rounded-lg border border-parchment-line bg-white px-5 pt-5 pb-7">
        <div className="flex items-center justify-between">
          <p className="text-sm text-slate-dim">{label}</p>
          <span
            className="flex h-9 w-9 items-center justify-center rounded-md"
            style={{
              backgroundColor: (failed ? COLORS.brick : accent ?? COLORS.brass) + '22',
              color: failed ? COLORS.brick : accent ?? COLORS.brass,
            }}
          >
            {failed ? <AlertTriangle size={18} /> : <Icon size={18} />}
          </span>
        </div>
        {failed ? (
          <p className="mt-3 text-sm font-medium text-brick">Couldn't load</p>
        ) : (
          <p className="mt-3 text-3xl font-semibold text-ink">
            {typeof value === 'number' ? <TallyCounter value={value} /> : value}
            {suffix && <span className="ml-1 text-lg font-medium text-slate-dim">{suffix}</span>}
          </p>
        )}
      </StampItem>
    </div>
  )
}

function PanelHeader({ icon: Icon, title, note }: { icon: typeof CalendarCheck2; title: string; note?: string }) {
  return (
    <div className="mb-4 pb-3">
      <div className="flex items-center gap-2">
        <Icon size={16} className="text-brass" />
        <h3 className="font-display text-base font-medium text-ink">{title}</h3>
      </div>
      {note && <p className="mt-1 text-xs italic text-slate-dim">{note}</p>}
      <LedgerRule className="mt-3" />
    </div>
  )
}

/** Shown inside a panel body when its backing request failed, distinct from
 *  a genuine "nothing here yet" empty state. */
function PanelError({ message = "Couldn't load this section. Try refreshing the page." }: { message?: string }) {
  return (
    <div className="flex items-start gap-2 text-sm text-brick">
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

/** Backend timetable keys are MONDAY..FRIDAY (uppercase). */
function todayScheduleKey() {
  return new Date().toLocaleDateString('en-US', { weekday: 'long' }).toUpperCase()
}

/* ------------------------------------------------------------------ */
/* Main component                                                     */
/* ------------------------------------------------------------------ */

export function StudentDashboard() {
  const [profile, setProfile] = useState<StudentProfile | null>(null)
  const [data, setData] = useState<DashboardData | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function load() {
      // Each real endpoint is fetched independently via allSettled - if one fails
      // (e.g. results aren't published yet, so /student/results 404s) the rest of
      // the dashboard still renders correctly with safe defaults for that section,
      // rather than the whole page falling back to made-up data.
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
        id: `${entry.subjectId}-${index}`,
        subject: entry.subjectName,
        time: `${entry.startTime} - ${entry.endTime}`,
        teacher: entry.teacherName,
        room: entry.room,
      }))

      const announcements: Announcement[] = notifications.map((n) => ({
        id: n.id,
        title: n.title,
        body: n.message,
        postedAt: n.createdAt,
      }))

      setData({
        attendancePercentage: Math.round(attendance?.overallAttendancePercentage ?? summary?.attendancePercentage ?? 0),
        subjectsCount: summary?.totalSubjects ?? 0,
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
        <div className="flex h-64 items-center justify-center gap-2 text-slate-dim">
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

  // Attendance stat/chart draw from either the dedicated attendance endpoint
  // or the summary endpoint as a fallback - only flag it as failed if both
  // sources came back rejected.
  const attendanceFailed = data.failed.attendance && data.failed.summary

  return (
    <Layout>
      <div className="relative mb-6">
        <h1 className="font-display text-2xl font-medium text-ink">
          Welcome{profile?.firstName ? `, ${profile.firstName}` : ''}
        </h1>
        <p className="mt-1 text-sm text-slate-dim">Here's what's happening with your studies.</p>

        <div className="stamp-in absolute -top-1 right-0 hidden sm:block">
          {photoUrl ? (
            <img
              src={photoUrl}
              alt="Your profile photo"
              className="h-14 w-14 rounded-full border-2 border-white object-cover shadow-[var(--shadow-paper-lift)]"
            />
          ) : (
            <div
              className="flex h-14 w-14 items-center justify-center rounded-full border-2 border-white font-display text-lg font-medium text-white shadow-[var(--shadow-paper-lift)]"
              style={{ backgroundColor: COLORS.brass }}
            >
              {initials}
            </div>
          )}
        </div>
      </div>

      {/* Stat cards */}
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
          label="Pending Work"
          value={data.pendingWorkCount}
          accent={STAT_SHADES[3]}
          failed={data.failed.assignments}
        />
        <StatCard
          icon={Award}
          label="Average Marks"
          value={data.averageMarks}
          suffix="%"
          accent={STAT_SHADES[4]}
          failed={data.failed.results}
        />
        <StatCard
          icon={Clock}
          label="Today's Classes"
          value={data.todayClasses.length}
          accent={STAT_SHADES[5]}
          failed={data.failed.timetable}
        />
        <StatCard
          icon={AlarmClockCheck}
          label="Upcoming Deadlines"
          value={data.upcomingDeadlines.length}
          accent={STAT_SHADES[6]}
          failed={data.failed.assignments}
        />
        <StatCard
          icon={Megaphone}
          label="Announcements"
          value={data.announcements.length}
          accent={STAT_SHADES[7]}
          failed={data.failed.notifications}
        />
      </StampGrid>

      {/* Charts */}
      <div className="mb-6 grid grid-cols-1 gap-4 lg:grid-cols-3">
        <motion.div
          className="paper rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)] lg:col-span-2"
          custom={0}
          variants={panelIn}
          initial="hidden"
          animate="show"
        >
          <PanelHeader icon={CalendarCheck2} title="Attendance by Subject" />
          {attendanceFailed ? (
            <PanelError />
          ) : data.attendanceBySubject.length === 0 ? (
            <p className="text-sm text-slate-dim">No attendance records yet.</p>
          ) : (
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={data.attendanceBySubject} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid stroke={COLORS.grid} vertical={false} />
                <XAxis dataKey="subject" tick={{ fontSize: 12, fill: COLORS.slateDim }} axisLine={false} tickLine={false} />
                <YAxis domain={[0, 100]} tick={{ fontSize: 12, fill: COLORS.slateDim }} axisLine={false} tickLine={false} />
                <Tooltip
                  contentStyle={{
                    borderRadius: 8,
                    border: `1px solid ${COLORS.grid}`,
                    fontSize: 13,
                  }}
                  formatter={(value) => [`${value}%`, 'Attendance']}
                  cursor={{ fill: COLORS.grid, opacity: 0.4 }}
                />
                <Bar dataKey="percentage" fill={COLORS.brass} radius={[4, 4, 0, 0]} animationDuration={700} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </motion.div>

        <motion.div
          className="paper rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)]"
          custom={1}
          variants={panelIn}
          initial="hidden"
          animate="show"
        >
          <PanelHeader icon={CalendarCheck2} title="Attendance Split" />
          {attendanceFailed ? (
            <PanelError />
          ) : (
            <>
              <ResponsiveContainer width="100%" height={240}>
                <PieChart>
                  <Pie
                    data={pieData}
                    dataKey="value"
                    nameKey="name"
                    innerRadius={60}
                    outerRadius={88}
                    paddingAngle={2}
                    startAngle={90}
                    endAngle={-270}
                    animationDuration={700}
                  >
                    {pieData.map((entry, index) => (
                      <Cell key={entry.name} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value) => `${value}%`} />
                </PieChart>
              </ResponsiveContainer>
              <p className="-mt-4 text-center text-2xl font-semibold text-ink">
                <TallyCounter value={data.attendancePercentage} format={(n) => `${Math.round(n)}%`} />
              </p>
              <p className="text-center text-xs text-slate-dim">Present this term</p>
            </>
          )}
        </motion.div>
      </div>

      <motion.div
        className="paper mb-6 rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)]"
        custom={2}
        variants={panelIn}
        initial="hidden"
        animate="show"
      >
        <PanelHeader
          icon={Award}
          title="Marks by Subject"
          note={!data.failed.results && data.subjectMarks.length === 0 ? 'No published results yet' : undefined}
        />
        {data.failed.results ? (
          <PanelError message="Couldn't load your results. Try refreshing the page." />
        ) : data.subjectMarks.length === 0 ? (
          <p className="text-sm text-slate-dim">Results will appear here once published.</p>
        ) : (
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={data.subjectMarks} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
              <CartesianGrid stroke={COLORS.grid} vertical={false} />
              <XAxis dataKey="subject" tick={{ fontSize: 12, fill: COLORS.slateDim }} axisLine={false} tickLine={false} />
              <YAxis domain={[0, 100]} tick={{ fontSize: 12, fill: COLORS.slateDim }} axisLine={false} tickLine={false} />
              <Tooltip
                contentStyle={{ borderRadius: 8, border: `1px solid ${COLORS.grid}`, fontSize: 13 }}
                formatter={(value) => [`${value}%`, 'Marks']}
                cursor={{ fill: COLORS.grid, opacity: 0.4 }}
              />
              <Bar dataKey="marks" fill={COLORS.brass} radius={[4, 4, 0, 0]} animationDuration={700} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </motion.div>

      {/* Lists */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2 xl:grid-cols-3">
        <motion.div
          className="paper rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)]"
          custom={3}
          variants={panelIn}
          initial="hidden"
          animate="show"
        >
          <PanelHeader
            icon={Clock}
            title="Today's Classes"
            note={
              !data.failed.timetable && data.timetableIsPlaceholder
                ? 'Provisional schedule — timetabling isn\'t final yet'
                : undefined
            }
          />
          {data.failed.timetable ? (
            <PanelError message="Couldn't load today's schedule. Try refreshing the page." />
          ) : data.todayClasses.length === 0 ? (
            <p className="text-sm text-slate-dim">No classes scheduled today.</p>
          ) : (
            <motion.ul className="space-y-3" variants={listStagger} initial="hidden" animate="show">
              {data.todayClasses.map((c) => (
                <motion.li
                  key={c.id}
                  variants={listItem}
                  className="flex items-start justify-between gap-3 text-sm"
                >
                  <div>
                    <p className="font-medium text-ink">{c.subject}</p>
                    <p className="text-xs text-slate-dim">
                      {c.teacher} &middot; {c.room}
                    </p>
                  </div>
                  <span className="whitespace-nowrap rounded bg-parchment px-2 py-1 font-mono text-xs text-slate">
                    {c.time}
                  </span>
                </motion.li>
              ))}
            </motion.ul>
          )}
        </motion.div>

        <motion.div
          className="paper rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)]"
          custom={4}
          variants={panelIn}
          initial="hidden"
          animate="show"
        >
          <PanelHeader icon={AlarmClockCheck} title="Upcoming Deadlines" />
          {data.failed.assignments ? (
            <PanelError message="Couldn't load your assignments. Try refreshing the page." />
          ) : data.upcomingDeadlines.length === 0 ? (
            <p className="text-sm text-slate-dim">Nothing due soon. Nice work.</p>
          ) : (
            <motion.ul className="space-y-3" variants={listStagger} initial="hidden" animate="show">
              {data.upcomingDeadlines.map((d) => (
                <motion.li
                  key={d.id}
                  variants={listItem}
                  className="flex items-start justify-between gap-3 text-sm"
                >
                  <div>
                    <p className="font-medium text-ink">{d.title}</p>
                    <p className="text-xs text-slate-dim">{d.subject}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs text-slate-dim">{formatDate(d.dueDate)}</p>
                    <span className="text-xs font-medium text-brick">{daysUntil(d.dueDate)}</span>
                  </div>
                </motion.li>
              ))}
            </motion.ul>
          )}
        </motion.div>

        <motion.div
          className="paper rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)] lg:col-span-2 xl:col-span-1"
          custom={5}
          variants={panelIn}
          initial="hidden"
          animate="show"
        >
          <PanelHeader icon={Megaphone} title="Recent Announcements" />
          {data.failed.notifications ? (
            <PanelError message="Couldn't load announcements. Try refreshing the page." />
          ) : data.announcements.length === 0 ? (
            <p className="text-sm text-slate-dim">No announcements yet.</p>
          ) : (
            <motion.ul className="space-y-4" variants={listStagger} initial="hidden" animate="show">
              {data.announcements.map((a) => (
                <motion.li
                  key={a.id}
                  variants={listItem}
                  className="border-b border-parchment-line pb-3 last:border-0 last:pb-0"
                >
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-medium text-ink">{a.title}</p>
                    <span className="text-xs text-slate-dim">{formatDate(a.postedAt)}</span>
                  </div>
                  <p className="mt-1 text-xs text-slate-dim">{a.body}</p>
                </motion.li>
              ))}
            </motion.ul>
          )}
        </motion.div>
      </div>
    </Layout>
  )
}