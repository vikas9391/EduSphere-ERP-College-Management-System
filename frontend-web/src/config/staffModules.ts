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
 * This is the single source of truth for both the sidebar (Layout.tsx) and the
 * dashboard tile grid (DashboardPage.tsx) - a permission added here shows up
 * consistently in both places instead of the two drifting out of sync.
 */
export interface StaffModule {
  to: string
  label: string
  icon: LucideIcon
  desc: string
  /** null = always visible to staff; otherwise at least one of these permissions is required. */
  permissions: string[] | null
}

export const staffModules: StaffModule[] = [
  {
    to: '/dashboard',
    label: 'Dashboard',
    icon: LayoutDashboard,
    desc: 'Your overview',
    permissions: null,
  },
  {
    to: '/departments',
    label: 'Departments',
    icon: Building2,
    desc: 'Manage academic departments',
    permissions: ['VIEW_DEPARTMENT'],
  },
  {
    to: '/courses',
    label: 'Courses',
    icon: BookOpen,
    desc: 'Manage college courses',
    permissions: ['VIEW_COURSE'],
  },
  {
    to: '/subjects',
    label: 'Subjects',
    icon: Layers,
    desc: 'Subjects offered in each course',
    permissions: ['VIEW_SUBJECT'],
  },
  {
    to: '/teachers',
    label: 'Teachers',
    icon: GraduationCap,
    desc: 'Faculty management',
    permissions: ['VIEW_TEACHER'],
  },
  {
    to: '/students',
    label: 'Students',
    icon: Users,
    desc: 'Student records',
    permissions: ['VIEW_STUDENT'],
  },
  {
    to: '/enrollments',
    label: 'Enrollments',
    icon: BookMarked,
    desc: 'Student subject enrollment',
    permissions: ['VIEW_ENROLLMENT', 'MANAGE_ENROLLMENT'],
  },
  {
    to: '/attendance',
    label: 'Attendance',
    icon: CalendarCheck,
    desc: 'Track daily attendance',
    permissions: ['VIEW_ATTENDANCE_REPORTS', 'MANAGE_ATTENDANCE'],
  },
  {
    to: '/assignments',
    label: 'Assignments',
    icon: ClipboardList,
    desc: 'Manage assignments',
    permissions: ['VIEW_ASSIGNMENTS', 'MANAGE_ASSIGNMENTS'],
  },
  {
    to: '/submissions',
    label: 'Submissions',
    icon: Upload,
    desc: 'Assignment submissions',
    permissions: ['VIEW_ASSIGNMENTS', 'MANAGE_ASSIGNMENTS'],
  },
  {
    to: '/exams',
    label: 'Examinations',
    icon: ClipboardCheck,
    desc: 'Schedule and manage exams',
    permissions: ['MANAGE_EXAMS'],
  },
  {
    to: '/results',
    label: 'Results',
    icon: Award,
    desc: 'Student results',
    permissions: ['VIEW_RESULTS'],
  },
  {
    to: '/users',
    label: 'Staff Accounts',
    icon: IdCard,
    desc: 'Admins, HODs, supervisors, and any other role you\'ve built',
    permissions: ['VIEW_USER'],
  },
  {
    to: '/roles',
    label: 'Roles',
    icon: ShieldCheck,
    desc: 'Build custom roles and permission sets',
    permissions: ['CREATE_ROLE', 'EDIT_ROLE', 'DELETE_ROLE', 'ASSIGN_ROLE'],
  },
]

/** True if the signed-in staff user's permissions unlock this module. */
export function canAccessModule(module: StaffModule, granted: string[]): boolean {
  if (module.permissions === null) return true
  return module.permissions.some((p) => granted.includes(p))
}
