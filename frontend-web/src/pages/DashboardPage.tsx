// src/pages/DashboardPage.tsx
import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Layout } from '@/components/Layout'
import { useAuthStore } from '@/store/authStore'
import { StampGrid, StampItem, LedgerRule } from '@/components/motion'
import { StatCard, PanelHeader, STAT_SHADES } from '@/components/PageBits'
import {
  Building2,
  BookOpen,
  Layers,
  GraduationCap,
  Users,
  ClipboardCheck,
  CalendarDays,
  FileText,
  BookMarked,
  BarChart3,
} from 'lucide-react'
import { getDepartments } from '@/api'
import { getCourses } from '@/api'
import { getTeachers } from '@/api'
import { getStudents } from '@/api'

const modules = [
  {
    to: '/departments',
    label: 'Departments',
    icon: Building2,
    desc: 'Manage academic departments',
  },
  {
    to: '/courses',
    label: 'Courses',
    icon: BookOpen,
    desc: 'Manage college courses',
  },
  {
    to: '/subjects',
    label: 'Subjects',
    icon: Layers,
    desc: 'Subjects offered in each course',
  },
  {
    to: '/teachers',
    label: 'Teachers',
    icon: GraduationCap,
    desc: 'Faculty management',
  },
  {
    to: '/students',
    label: 'Students',
    icon: Users,
    desc: 'Student records',
  },
  {
    to: '/enrollments',
    label: 'Enrollments',
    icon: ClipboardCheck,
    desc: 'Student subject enrollment',
  },
  {
    to: '/attendance',
    label: 'Attendance',
    icon: CalendarDays,
    desc: 'Track daily attendance',
  },
  {
    to: '/assignments',
    label: 'Assignments',
    icon: FileText,
    desc: 'Manage assignments',
  },
  {
    to: '/submissions',
    label: 'Submissions',
    icon: BookMarked,
    desc: 'Assignment submissions',
  },
]

const EASE_STAMP = [0.16, 1, 0.3, 1] as const

const panelIn = {
  hidden: { opacity: 0, y: 14 },
  show: (i: number) => ({
    opacity: 1,
    y: 0,
    transition: { duration: 0.4, ease: EASE_STAMP, delay: 0.06 * i },
  }),
}

interface OverviewCounts {
  departments: number
  courses: number
  teachers: number
  students: number
  failed: {
    departments: boolean
    courses: boolean
    teachers: boolean
    students: boolean
  }
}

// The admin module grid. Exported separately so it can also be mounted
// directly at /admin/dashboard.
export function AdminDashboard() {
  const { user } = useAuthStore()
  const [counts, setCounts] = useState<OverviewCounts | null>(null)

  useEffect(() => {
    let cancelled = false

    async function load() {
      // Each count is fetched independently via allSettled - if one endpoint
      // fails, the rest of the overview still renders with real numbers
      // instead of the whole row falling back to a placeholder.
      const [deptResult, courseResult, teacherResult, studentResult] = await Promise.allSettled([
        getDepartments(),
        getCourses(),
        getTeachers(),
        getStudents(),
      ])

      if (cancelled) return

      setCounts({
        departments: deptResult.status === 'fulfilled' ? deptResult.value.length : 0,
        courses: courseResult.status === 'fulfilled' ? courseResult.value.length : 0,
        teachers: teacherResult.status === 'fulfilled' ? teacherResult.value.length : 0,
        students: studentResult.status === 'fulfilled' ? studentResult.value.length : 0,
        failed: {
          departments: deptResult.status === 'rejected',
          courses: courseResult.status === 'rejected',
          teachers: teacherResult.status === 'rejected',
          students: studentResult.status === 'rejected',
        },
      })
    }

    load()
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <Layout>
      <h1 className="font-display text-2xl font-medium text-ink">Welcome back</h1>
      <p className="mt-2 text-sm text-slate-dim">
        Signed in as {user?.email} · {user?.role}
      </p>

      <StampGrid className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {modules.map(({ to, label, icon: Icon, desc }) => (
          <StampItem key={to} to={to} className="block rounded-lg border border-parchment-line bg-white/50 p-5">
            <Icon size={20} className="text-brass" />
            <p className="mt-3 font-display text-base font-medium text-ink">{label}</p>
            <p className="mt-1 text-sm text-slate-dim">{desc}</p>
          </StampItem>
        ))}
      </StampGrid>

      <LedgerRule className="mt-10 w-full" />

      <motion.div className="mt-6" custom={0} variants={panelIn} initial="hidden" animate="show">
        <PanelHeader icon={BarChart3} title="Institution Overview" />

        <StampGrid className="grid grid-cols-2 gap-4 md:grid-cols-4">
          <StatCard
            icon={Building2}
            label="Departments"
            value={counts?.departments ?? 0}
            accent={STAT_SHADES[0]}
            failed={counts?.failed.departments}
          />
          <StatCard
            icon={BookOpen}
            label="Courses"
            value={counts?.courses ?? 0}
            accent={STAT_SHADES[2]}
            failed={counts?.failed.courses}
          />
          <StatCard
            icon={GraduationCap}
            label="Teachers"
            value={counts?.teachers ?? 0}
            accent={STAT_SHADES[4]}
            failed={counts?.failed.teachers}
          />
          <StatCard
            icon={Users}
            label="Students"
            value={counts?.students ?? 0}
            accent={STAT_SHADES[6]}
            failed={counts?.failed.students}
          />
        </StampGrid>
      </motion.div>
    </Layout>
  )
}

// /dashboard — there's no SUPER_ADMIN role in the backend, so this is just
// the regular admin dashboard. (Previously branched on a SUPER_ADMIN role
// to show a dashboard picker — removed since that role doesn't exist.)
export function DashboardPage() {
  return <AdminDashboard />
}