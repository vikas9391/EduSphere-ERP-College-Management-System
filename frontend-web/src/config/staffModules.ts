import {
  LayoutDashboard, Building2, BookOpen, Layers, GraduationCap, Users, ClipboardCheck, CalendarCheck,
  ClipboardList, Upload, Award, BookMarked, ShieldCheck, IdCard, Megaphone, type LucideIcon,
} from 'lucide-react'

export interface StaffModule {
  to: string
  label: string
  icon: LucideIcon
  desc: string
  permissions: string[] | null
  category: 'administration' | 'operational'
}

export const staffModules: StaffModule[] = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard, desc: 'Your overview', permissions: null, category: 'administration' },
  { to: '/announcements', label: 'Announcements', icon: Megaphone, desc: 'Send and receive college announcements', permissions: ['VIEW_ANNOUNCEMENT', 'CREATE_ANNOUNCEMENT'], category: 'administration' },
  { to: '/departments', label: 'Departments', icon: Building2, desc: 'Manage academic departments', permissions: ['VIEW_DEPARTMENT'], category: 'operational' },
  { to: '/courses', label: 'Courses', icon: BookOpen, desc: 'Manage college courses', permissions: ['VIEW_COURSE'], category: 'operational' },
  { to: '/subjects', label: 'Subjects', icon: Layers, desc: 'Subjects offered in each course', permissions: ['VIEW_SUBJECT'], category: 'operational' },
  { to: '/teachers', label: 'Teachers', icon: GraduationCap, desc: 'Faculty management', permissions: ['VIEW_TEACHER'], category: 'operational' },
  { to: '/students', label: 'Students', icon: Users, desc: 'Student records', permissions: ['VIEW_STUDENT'], category: 'operational' },
  { to: '/enrollments', label: 'Enrollments', icon: BookMarked, desc: 'Student subject enrollment', permissions: ['VIEW_ENROLLMENT', 'MANAGE_ENROLLMENT'], category: 'operational' },
  { to: '/attendance', label: 'Attendance', icon: CalendarCheck, desc: 'Track daily attendance', permissions: ['VIEW_ATTENDANCE_REPORTS', 'MANAGE_ATTENDANCE'], category: 'operational' },
  { to: '/assignments', label: 'Assignments', icon: ClipboardList, desc: 'Manage assignments', permissions: ['VIEW_ASSIGNMENTS', 'MANAGE_ASSIGNMENTS'], category: 'operational' },
  { to: '/submissions', label: 'Submissions', icon: Upload, desc: 'Assignment submissions', permissions: ['VIEW_ASSIGNMENTS', 'MANAGE_ASSIGNMENTS'], category: 'operational' },
  { to: '/exams', label: 'Examinations', icon: ClipboardCheck, desc: 'Schedule and manage exams', permissions: ['MANAGE_EXAMS'], category: 'operational' },
  { to: '/results', label: 'Results', icon: Award, desc: 'Student results', permissions: ['VIEW_RESULTS'], category: 'operational' },
  { to: '/users', label: 'Staff Accounts', icon: IdCard, desc: 'Admins, HODs, supervisors, and other staff roles', permissions: ['VIEW_USER'], category: 'administration' },
  { to: '/roles', label: 'Roles', icon: ShieldCheck, desc: 'Build custom roles and permission sets', permissions: ['CREATE_ROLE', 'EDIT_ROLE', 'DELETE_ROLE', 'ASSIGN_ROLE'], category: 'administration' },
]

export function canAccessModule(module: StaffModule, granted: string[]): boolean {
  if (module.permissions === null) return true
  return module.permissions.some((p) => granted.includes(p))
}

export function navModulesForRole(role: string | undefined, granted: string[]): StaffModule[] {
  if (role === 'ADMIN') return staffModules.filter((module) => module.category === 'administration')
  return staffModules.filter((module) => canAccessModule(module, granted))
}
