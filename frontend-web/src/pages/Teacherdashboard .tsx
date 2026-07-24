// src/pages/TeacherDashboard.tsx
import { useEffect, useState } from 'react'
import { Layout } from '@/components/Layout'
import { useAuthStore } from '@/store/authStore'
import { StampGrid } from '@/components/motion'
import { StatCard, PanelHeader, PanelError, COLORS, STAT_SHADES } from '@/components/PageBits'
import {
  CalendarDays,
  BookOpen,
  Users,
  ClipboardList,
  ClipboardCheck,
  Clock,
  Megaphone,
  MapPin,
  ChevronRight,
  Award,
} from 'lucide-react'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
  AreaChart,
  Area,
} from 'recharts'
import { getTeacherDashboardSummary, type TeacherDashboardSummary } from '@/api/teacherPortal'

export function TeacherDashboard() {
  const { user } = useAuthStore()
  const [summary, setSummary] = useState<TeacherDashboardSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let mounted = true
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const data = await getTeacherDashboardSummary()
        if (mounted) setSummary(data)
      } catch {
        if (mounted) setError('Failed to load dashboard data. Please try again.')
      } finally {
        if (mounted) setLoading(false)
      }
    }
    load()
    return () => {
      mounted = false
    }
  }, [user?.id])

  const stats = [
    { label: "Today's Classes", value: summary?.upcomingClassesCount ?? 0, icon: CalendarDays },
    { label: 'Subjects', value: summary?.totalSubjects ?? 0, icon: BookOpen },
    { label: 'Students', value: summary?.totalStudents ?? 0, icon: Users },
    { label: 'Assignments Pending', value: summary?.pendingReviewCount ?? 0, icon: ClipboardList },
    { label: 'Attendance Pending', value: summary?.attendancePendingToday ?? 0, icon: ClipboardCheck },
    { label: 'Upcoming Classes', value: summary?.upcomingClassesCount ?? 0, icon: Clock },
  ]

  const assignmentsPerSubject = (summary?.assignmentsPerSubject ?? []).map((s) => ({
    name: s.subjectName,
    count: s.count,
  }))

  const attendanceTrend = (summary?.attendanceTrend ?? []).map((p) => ({
    label: p.label,
    rate: p.ratePercentage,
  }))

  const todaysSchedule = summary?.todaysSchedule ?? []
  const recentAssignments = summary?.recentAssignments ?? []
  const announcements = summary?.announcements ?? []

  return (
    <Layout>
      <h1 className="font-display text-2xl font-medium text-ink">
        Welcome back{user?.email ? `, ${user.email.split('@')[0]}` : ''}
      </h1>
      <p className="mt-1 text-sm text-slate-dim">Here's what's happening in your classes today</p>

      {error && <PanelError message={error} />}

      {/* Stat cards */}
      <StampGrid className="mt-6 grid grid-cols-2 gap-4 lg:grid-cols-3 xl:grid-cols-6">
        {stats.map(({ label, value, icon: Icon }, i) => (
          <StatCard
            key={label}
            icon={Icon}
            label={label}
            value={loading ? '—' : value}
            accent={STAT_SHADES[i % STAT_SHADES.length]}
          />
        ))}
      </StampGrid>

      {/* Charts */}
      <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="paper rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)]">
          <PanelHeader icon={ClipboardList} title="Assignments by Subject" note="How your assignments are distributed" />
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={assignmentsPerSubject}>
                <CartesianGrid strokeDasharray="3 3" stroke={COLORS.grid} vertical={false} />
                <XAxis dataKey="name" tick={{ fontSize: 12, fill: COLORS.slateDim }} axisLine={false} tickLine={false} />
                <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: COLORS.slateDim }} axisLine={false} tickLine={false} />
                <Tooltip
                  contentStyle={{
                    background: '#fffdf7',
                    border: `1px solid ${COLORS.grid}`,
                    borderRadius: 8,
                    fontSize: 12,
                  }}
                  cursor={{ fill: COLORS.grid, opacity: 0.4 }}
                />
                <Bar dataKey="count" fill={COLORS.brass} radius={[4, 4, 0, 0]} animationDuration={700} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="paper rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)]">
          <PanelHeader icon={CalendarDays} title="Attendance Trend" note="Average attendance rate, last 7 days" />
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={attendanceTrend}>
                <defs>
                  <linearGradient id="attendanceFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor={COLORS.brass} stopOpacity={0.4} />
                    <stop offset="100%" stopColor={COLORS.brass} stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke={COLORS.grid} vertical={false} />
                <XAxis dataKey="label" tick={{ fontSize: 12, fill: COLORS.slateDim }} axisLine={false} tickLine={false} />
                <YAxis domain={[0, 100]} tick={{ fontSize: 12, fill: COLORS.slateDim }} axisLine={false} tickLine={false} />
                <Tooltip
                  formatter={(value) => [`${Number(value ?? 0)}%`, 'Attendance']}
                  contentStyle={{
                    background: '#fffdf7',
                    border: `1px solid ${COLORS.grid}`,
                    borderRadius: 8,
                    fontSize: 12,
                  }}
                  cursor={{ fill: COLORS.grid, opacity: 0.4 }}
                />
                <Area type="monotone" dataKey="rate" stroke={COLORS.brass} strokeWidth={2} fill="url(#attendanceFill)" animationDuration={700} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Schedule / Recent Assignments / Announcements */}
      <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Today's Schedule */}
        <div className="paper rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)]">
          <PanelHeader
            icon={CalendarDays}
            title="Today's Schedule"
            note={summary?.schedulePlaceholder ? 'Placeholder times - no timetable module yet' : undefined}
          />

          <div className="space-y-3">
            {loading ? (
              <p className="text-sm text-slate-dim">Loading...</p>
            ) : todaysSchedule.length === 0 ? (
              <p className="text-sm text-slate-dim">No classes scheduled for today.</p>
            ) : (
              todaysSchedule.map((entry) => (
                <div
                  key={entry.subjectId}
                  className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-parchment-line/70 px-3 py-2"
                >
                  <div>
                    <p className="text-sm font-medium text-ink">{entry.subjectName}</p>
                    <p className="mt-0.5 flex items-center gap-1 text-xs text-slate-dim">
                      <MapPin size={11} />
                      {entry.room}
                    </p>
                  </div>
                  <span className="shrink-0 rounded-full bg-parchment-line/50 px-2.5 py-1 text-xs text-slate-dim">
                    {entry.startTime} – {entry.endTime}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Recent Assignments */}
        <div className="paper rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)]">
          <PanelHeader icon={ClipboardList} title="Recent Assignments" />

          <div className="space-y-3">
            {loading ? (
              <p className="text-sm text-slate-dim">Loading...</p>
            ) : recentAssignments.length === 0 ? (
              <p className="text-sm text-slate-dim">You haven't created any assignments yet.</p>
            ) : (
              recentAssignments.map((a) => (
                <div key={a.assignmentId} className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-parchment-line/70 px-3 py-2">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-ink">{a.title}</p>
                    <p className="mt-0.5 text-xs text-slate-dim">{a.subjectName}</p>
                  </div>
                  <div className="flex shrink-0 items-center gap-3 text-xs text-slate-dim">
                    <span className="inline-flex items-center gap-1">
                      <Award size={11} />
                      {a.maxMarks}
                    </span>
                    <span>{a.dueDate.slice(0, 10)}</span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Announcements */}
        <div className="paper rounded-lg border border-parchment-line bg-white/60 p-5 shadow-[var(--shadow-paper-lift)]">
          <PanelHeader
            icon={Megaphone}
            title="Announcements"
            note={summary?.announcementsPlaceholder ? 'Sample content - no announcements module yet' : undefined}
          />

          <div className="space-y-3">
            {announcements.map((n) => (
              <div key={n.id} className="group rounded-md border border-parchment-line/70 px-3 py-2">
                <div className="flex items-start justify-between gap-2">
                  <p className="text-sm font-medium text-ink">{n.title}</p>
                  <ChevronRight size={14} className="mt-0.5 shrink-0 text-slate-dim opacity-0 transition-opacity group-hover:opacity-100" />
                </div>
                <p className="mt-1 text-xs text-slate-dim">{n.body}</p>
                <p className="mt-2 text-[11px] uppercase tracking-wide text-slate-dim/70">{n.createdAt.slice(0, 10)}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </Layout>
  )
}
