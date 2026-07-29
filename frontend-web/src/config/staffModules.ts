// src/config/staffModules.ts
import {
  LayoutDashboard,
  Building2,
  BookOpen,
  Layers,
  GraduationCap,
  Users,
  ClipboardCheck,
  CalendarCheck,
  ClipboardList,
  Upload,
  Award,
  BookMarked,
  ShieldCheck,
  IdCard,
  type LucideIcon,
} from 'lucide-react'

/**
 * Every module a staff/admin account (ADMIN or any custom Role built via the
 * Role/Permission system - HOD, Supervisor, Accountant, whatever an admin creates)
 * might see, alongside the Permission(s) that unlock it.
 *
 * `permissions: null` means always visible to any signed-in staff account (there's
 * nothing to gate - e.g. the Dashboard link itself). Everything else requires at
 * least one of the listed permissions, mirroring the backend's own
 * `@PreAuthorize("hasAnyAuthority(...)")` checks so the UI a role sees lines up with
 * what it can actually do once it gets there.
 *
 * `category` separates the head-of-institution's own areas ('administration' -
 * Dashboard, Roles, Staff Accounts) from the day-to-day work areas a dedicated staff
 * role is meant to run ('operational' - Departments, Attendance, Exams, etc.). See
 * {@link navModulesForRole} for how this is used.
 *
 * This is the single source of truth for both the sidebar (Layout.tsx) and the
 * dashboard tile grid (DashboardPage.tsx) - a permission (or category) added here
 * shows up consistently in both places instead of the two drifting out of sync.
 */
export interface StaffModule {
  to: string
  label: string
  icon: LucideIcon
  desc: string
  /** null = always visible to staff; otherwise at least one of these permissions is required. */
  permissions: string[] | null
  category: 'administration' | 'operational'
}

export const staffModules: StaffModule[] = [
  {
    to: '/dashboard',
    label: 'Dashboard',
    icon: LayoutDashboard,
    desc: 'Your overview',
    permissions: null,
    category: 'administration',
  },
  {
    to: '/departments',
    label: 'Departments',
    icon: Building2,
    desc: 'Manage academic departments',
    permissions: ['VIEW_DEPARTMENT'],
    category: 'operational',
  },
  {
    to: '/courses',
    label: 'Courses',
    icon: BookOpen,
    desc: 'Manage college courses',
    permissions: ['VIEW_COURSE'],
    category: 'operational',
  },
  {
    to: '/subjects',
    label: 'Subjects',
    icon: Layers,
    desc: 'Subjects offered in each course',
    permissions: ['VIEW_SUBJECT'],
    category: 'operational',
  },
  {
    to: '/teachers',
    label: 'Teachers',
    icon: GraduationCap,
    desc: 'Faculty management',
    permissions: ['VIEW_TEACHER'],
    category: 'operational',
  },
  {
    to: '/students',
    label: 'Students',
    icon: Users,
    desc: 'Student records',
    permissions: ['VIEW_STUDENT'],
    category: 'operational',
  },
  {
    to: '/enrollments',
    label: 'Enrollments',
    icon: BookMarked,
    desc: 'Student subject enrollment',
    permissions: ['VIEW_ENROLLMENT', 'MANAGE_ENROLLMENT'],
    category: 'operational',
  },
  {
    to: '/attendance',
    label: 'Attendance',
    icon: CalendarCheck,
    desc: 'Track daily attendance',
    permissions: ['VIEW_ATTENDANCE_REPORTS', 'MANAGE_ATTENDANCE'],
    category: 'operational',
  },
  {
    to: '/assignments',
    label: 'Assignments',
    icon: ClipboardList,
    desc: 'Manage assignments',
    permissions: ['VIEW_ASSIGNMENTS', 'MANAGE_ASSIGNMENTS'],
    category: 'operational',
  },
  {
    to: '/submissions',
    label: 'Submissions',
    icon: Upload,
    desc: 'Assignment submissions',
    permissions: ['VIEW_ASSIGNMENTS', 'MANAGE_ASSIGNMENTS'],
    category: 'operational',
  },
  {
    to: '/exams',
    label: 'Examinations',
    icon: ClipboardCheck,
    desc: 'Schedule and manage exams',
    permissions: ['MANAGE_EXAMS'],
    category: 'operational',
  },
  {
    to: '/results',
    label: 'Results',
    icon: Award,
    desc: 'Student results',
    permissions: ['VIEW_RESULTS'],
    category: 'operational',
  },
  {
    to: '/users',
    label: 'Staff Accounts',
    icon: IdCard,
    desc: 'Admins, HODs, supervisors, and any other role you\'ve built',
    permissions: ['VIEW_USER'],
    category: 'administration',
  },
  {
    to: '/roles',
    label: 'Roles',
    icon: ShieldCheck,
    desc: 'Build custom roles and permission sets',
    permissions: ['CREATE_ROLE', 'EDIT_ROLE', 'DELETE_ROLE', 'ASSIGN_ROLE'],
    category: 'administration',
  },
]

/** True if the signed-in staff user's permissions unlock this module. */
export function canAccessModule(module: StaffModule, granted: string[]): boolean {
  if (module.permissions === null) return true
  return module.permissions.some((p) => granted.includes(p))
}

/**
 * The modules a signed-in staff account's sidebar/dashboard tiles should show.
 *
 * ADMIN is seeded with every permission that exists (V19 migration) - it has to be,
 * since {@code RoleService#guardAgainstEscalation} only lets an account grant
 * permissions it holds itself, and ADMIN is the one creating every custom role. That
 * made ADMIN's own sidebar show every operational tab too (Attendance, Courses,
 * Departments, ...), even though that day-to-day work is meant to be run by a
 * dedicated staff role built for it, with the head account just overseeing things.
 *
 * So permission alone isn't the right gate for what ADMIN itself sees: ADMIN is
 * narrowed to 'administration' modules only (Dashboard, Roles, Staff Accounts) here,
 * regardless of which permissions it holds - it still gets the full Institution
 * Overview stat cards on the dashboard (see DashboardPage.tsx), just not a direct tab
 * for every module. Any other staff role (custom, built via the Role/Permission
 * system - HOD, Supervisor, Accountant, ...) is unaffected: it still sees whatever
 * its granted permissions unlock, operational modules included, exactly as before.
 */
export function navModulesForRole(role: string | undefined, granted: string[]): StaffModule[] {
  if (role === 'ADMIN') {
    return staffModules.filter((module) => module.category === 'administration')
  }
  return staffModules.filter((module) => canAccessModule(module, granted))
}
