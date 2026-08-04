// src/pages/DashboardPage.tsx
import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Layout } from '@/components/Layout'
import { useAuthStore, hasAnyPermission } from '@/store/authStore'
import { navModulesForRole } from '@/config/staffModules'
import { StampGrid, StampItem, LedgerRule } from '@/components/motion'
import { StatCard, PanelHeader, STAT_SHADES } from '@/components/PageBits'
import { Building2, BookOpen, GraduationCap, Users, BarChart3, type LucideIcon } from 'lucide-react'
import { getDepartments } from '@/api'
import { getCourses } from '@/api'
import { getTeachers } from '@/api'
import { getStudents } from '@/api'

interface StatConfig {
  key: 'departments' | 'courses' | 'teachers' | 'students'
  label: string
  icon: LucideIcon
  accent: string
  permissions: string[]
  fetch: () => Promise<{ length: number }>
}

// Each overview stat is gated on the same permission that unlocks its module page -
// no point showing (or fetching) a "Teachers" count for a role that can't open the
// Teachers page at all. Keeps this dashboard honest for a Supervisor-style role that
// only has a couple of VIEW_* permissions, instead of always hitting every endpoint
// and either showing a wall of "0"s or a wall of failed-fetch placeholders. ADMIN
// holds every permission (V19 migration), so this list stays fully populated for it -
// that's what gives the head account its "overview of everything" even though its
// module tiles below are narrowed to administration-only.
const statConfigs: StatConfig[] = [
  {
    key: 'departments',
    label: 'Departments',
    icon: Building2,
    accent: STAT_SHADES[0],
    permissions: ['VIEW_DEPARTMENT'],
    fetch: getDepartments,
  },
  {
    key: 'courses',
    label: 'Courses',
    icon: BookOpen,
    accent: STAT_SHADES[2],
    permissions: ['VIEW_COURSE'],
    fetch: getCourses,
  },
  {
    key: 'teachers',
    label: 'Teachers',
    icon: GraduationCap,
    accent: STAT_SHADES[4],
    permissions: ['VIEW_TEACHER'],
    fetch: getTeachers,
  },
  {
    key: 'students',
    label: 'Students',
    icon: Users,
    accent: STAT_SHADES[6],
    permissions: ['VIEW_STUDENT'],
    fetch: getStudents,
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

type StatKey = StatConfig['key']

interface OverviewCounts {
  values: Partial<Record<StatKey, number>>
  failed: Partial<Record<StatKey, boolean>>
}

/**
 * The staff/admin overview - module tile grid plus a quick institution snapshot.
 * Shared by every staff-side account (ADMIN or any custom Role an admin built).
 * <p>
 * The tile grid uses {@code navModulesForRole}, the same source that drives the
 * sidebar in Layout.tsx: ADMIN only ever sees its 'administration' tiles (Roles,
 * Staff Accounts) since the operational modules are meant to be run by whichever
 * staff role was built for that work, not the head account. Any other staff role
 * still sees whatever its granted permissions unlock, operational tiles included - a
 * Supervisor role with only VIEW_ATTENDANCE_REPORTS + VIEW_TEACHER_PROGRESS sees a
 * small, relevant set of tiles instead of the full admin control panel.
 * <p>
 * The Institution Overview stat cards below are unaffected by that narrowing - they
 * stay purely permission-gated, so ADMIN (which holds every permission) still gets a
 * full read-only snapshot of departments/courses/teachers/students right here without
 * needing a direct tab into each one.
 * <p>
 * Exported separately so it can also be mounted directly at /admin/dashboard.
 */
export function AdminDashboard() {
  const { user } = useAuthStore()
  const permissions = user?.permissions ?? []
  const [counts, setCounts] = useState<OverviewCounts | null>(null)

  const visibleModules = navModulesForRole(user?.role, permissions).filter((module) => module.to !== '/dashboard')
  const visibleStats = statConfigs.filter((stat) => hasAnyPermission(stat.permissions))

  useEffect(() => {
    let cancelled = false

    async function load() {
      if (visibleStats.length === 0) {
        setCounts({ values: {}, failed: {} })
        return
      }

      // Each count is fetched independently via allSettled - if one endpoint fails,
      // the rest of the overview still renders with real numbers instead of the
      // whole row falling back to a placeholder.
      const results = await Promise.allSettled(visibleStats.map((stat) => stat.fetch()))
      if (cancelled) return

      const values: Partial<Record<StatKey, number>> = {}
      const failed: Partial<Record<StatKey, boolean>> = {}

      visibleStats.forEach((stat, i) => {
        const result = results[i]
        values[stat.key] = result.status === 'fulfilled' ? result.value.length : 0
        failed[stat.key] = result.status === 'rejected'
      })

      setCounts({ values, failed })
    }

    load()
    return () => {
      cancelled = true
    }
    // permissions is a stable reference for the lifetime of a session (only changes on
    // login/refresh), so it's safe as the effect's dependency instead of the derived
    // visibleStats array, which would otherwise be a new array every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [permissions])

  return (
    <Layout>
      <h1 className="font-heading text-2xl font-medium text-text">Welcome back</h1>
      <p className="mt-2 text-sm text-muted">
        Signed in as {user?.email} · {user?.role}
      </p>

      {visibleModules.length > 0 ? (
        <StampGrid className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {visibleModules.map(({ to, label, icon: Icon, desc }) => (
            <StampItem key={to} to={to} className="block rounded-lg border border-border bg-white/50 p-5">
              <Icon size={20} className="text-primary" />
              <p className="mt-3 font-heading text-base font-medium text-text">{label}</p>
              <p className="mt-1 text-sm text-muted">{desc}</p>
            </StampItem>
          ))}
        </StampGrid>
      ) : (
        <p className="mt-8 text-sm text-muted">
          Your role doesn't have any modules assigned yet - ask an administrator to grant permissions.
        </p>
      )}

      {visibleStats.length > 0 && (
        <>
          <LedgerRule className="mt-10 w-full" />

          <motion.div className="mt-6" custom={0} variants={panelIn} initial="hidden" animate="show">
            <PanelHeader icon={BarChart3} title="Institution Overview" />

            <StampGrid className="grid grid-cols-2 gap-4 md:grid-cols-4">
              {visibleStats.map((stat) => (
                <StatCard
                  key={stat.key}
                  icon={stat.icon}
                  label={stat.label}
                  value={counts?.values[stat.key] ?? 0}
                  accent={stat.accent}
                  failed={counts?.failed[stat.key]}
                />
              ))}
            </StampGrid>
          </motion.div>
        </>
      )}
    </Layout>
  )
}

// /dashboard — there's no SUPER_ADMIN role in the backend, so this is just
// the regular admin/staff dashboard. (Previously branched on a SUPER_ADMIN role
// to show a dashboard picker — removed since that role doesn't exist.)
export function DashboardPage() {
  return <AdminDashboard />
}
