// src/components/Layout.tsx
import { useEffect, useState, type ReactNode } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { useAuthStore } from '@/store/authStore'
import { PageIn } from '@/components/motion'
import {
  LayoutDashboard,
  Building2,
  BookOpen,
  Layers,
  GraduationCap,
  Users,
  ClipboardCheck,
  Award,
  CalendarCheck,
  ClipboardList,
  Upload,
  BookMarked,
  UserCircle,
  Menu,
  X,
} from 'lucide-react'

const superAdminNavItems = [
  { to: '/colleges', label: 'Colleges', icon: Building2 },
]

/**
 * Nav items are role-scoped. Previously every signed-in user - admin, teacher, or
 * student - saw the exact same sidebar (full admin CRUD: Departments/Courses/
 * Subjects/Teachers/Students/Exams/Results), regardless of role. A teacher or student
 * logging in landed on their own dashboard but then had a sidebar pointing at pages
 * meant for administrators. The backend doesn't currently enforce role-based
 * authorization on those admin endpoints (see README_PROGRESS.md), so this is a
 * UX/least-privilege fix on the frontend rather than a security boundary - but it's
 * still the right default: don't hand a teacher a "Delete any student" button they
 * were never meant to see.
 */
const adminNavItems = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/departments', label: 'Departments', icon: Building2 },
  { to: '/courses', label: 'Courses', icon: BookOpen },
  { to: '/subjects', label: 'Subjects', icon: Layers },
  { to: '/teachers', label: 'Teachers', icon: GraduationCap },
  { to: '/students', label: 'Students', icon: Users },
  { to: '/enrollments', label: 'Enrollments', icon: BookMarked },
  { to: '/attendance', label: 'Attendance', icon: CalendarCheck },
  { to: '/assignments', label: 'Assignments', icon: ClipboardList },
  { to: '/submissions', label: 'Submissions', icon: Upload },
  { to: '/exams', label: 'Examinations', icon: ClipboardCheck },
  { to: '/results', label: 'Results', icon: Award },
]

const teacherNavItems = [
  { to: '/teacher/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/teacher/classes', label: 'Classes', icon: Layers },
  { to: '/attendance', label: 'Attendance', icon: CalendarCheck },
  { to: '/assignments', label: 'Assignments', icon: ClipboardList },
  { to: '/submissions', label: 'Submissions', icon: Upload },
  { to: '/results', label: 'Results', icon: Award },
]

const studentNavItems = [
  { to: '/student/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/student/profile', label: 'My Profile', icon: UserCircle },
  { to: '/student/classes', label: 'My Classes', icon: Users },
  { to: '/student/enrollments', label: 'My Enrollments', icon: BookMarked },
  { to: '/student/attendance', label: 'My Attendance', icon: CalendarCheck },
  { to: '/student/assignments', label: 'My Assignments', icon: ClipboardList },
]

function navItemsForRole(role: string | undefined) {
  switch (role) {
    case 'SUPER_ADMIN':
      return superAdminNavItems
    case 'TEACHER':
      return teacherNavItems
    case 'STUDENT':
      return studentNavItems
    default:
      // ADMIN, or an unrecognized role - fall back to the full admin view rather
      // than hiding navigation entirely.
      return adminNavItems
  }
}

export function Layout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuthStore()
  const navItems = navItemsForRole(user?.role)
  const location = useLocation()
  const [mobileNavOpen, setMobileNavOpen] = useState(false)

  // Close the mobile drawer automatically whenever the route changes, so
  // tapping a nav link doesn't leave the drawer open over the new page.
  useEffect(() => {
    setMobileNavOpen(false)
  }, [location.pathname])

  return (
    <div className="flex min-h-screen bg-parchment font-body">
      {/* Mobile menu toggle - only shown below md, where the sidebar is
          hidden off-screen by default. */}
      <button
        onClick={() => setMobileNavOpen((open) => !open)}
        aria-label={mobileNavOpen ? 'Close menu' : 'Open menu'}
        aria-expanded={mobileNavOpen}
        className="fixed left-4 top-4 z-40 flex h-10 w-10 items-center justify-center rounded-md border border-parchment-line bg-white shadow-[var(--shadow-paper)] md:hidden"
      >
        {mobileNavOpen ? <X size={18} /> : <Menu size={18} />}
      </button>

      {/* Backdrop behind the drawer on mobile - tapping it closes the nav. */}
      <AnimatePresence>
        {mobileNavOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            onClick={() => setMobileNavOpen(false)}
            className="fixed inset-0 z-20 bg-ink/40 md:hidden"
          />
        )}
      </AnimatePresence>

      {/* Sidebar is fixed to the viewport (not a normal flex child) so it
          stays put while the main content scrolls underneath it. On mobile
          it's an off-canvas drawer that slides in via translate-x; from md
          up it's always visible at translate-x-0. The content column below
          carries a matching md:ml-56 to avoid being hidden behind it. */}
      <aside
        className={`fixed inset-y-0 left-0 z-30 flex w-56 flex-col overflow-y-auto bg-parchment py-6 pl-3.5 pr-0 transition-transform duration-300 ease-in-out md:translate-x-0 ${
          mobileNavOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <span className="mb-8 pl-4 font-display text-lg font-medium text-ink">College ERP</span>
        <nav className="flex flex-1 flex-col gap-1.5">
          {navItems.map(({ to, label, icon: Icon }) => {
            const isActive =
              to === location.pathname ||
              (to !== '/dashboard' &&
                to !== '/teacher/dashboard' &&
                to !== '/student/dashboard' &&
                location.pathname.startsWith(to))
            return (
              <NavLink
                key={to}
                to={to}
                end={to === '/dashboard' || to === '/teacher/dashboard' || to === '/student/dashboard'}
                data-active={isActive}
                className="nav-tab flex items-center gap-2.5 border border-parchment-line bg-parchment-dim py-2 pl-4 pr-5 text-sm text-slate-dim data-[active=true]:z-10 data-[active=true]:border-brass/40 data-[active=true]:bg-white data-[active=true]:font-medium data-[active=true]:text-ink"
              >
                {isActive && <span className="nav-tab-dot absolute left-2 h-1.5 w-1.5 rounded-full bg-brick" />}
                <Icon size={16} />
                {label}
              </NavLink>
            )
          })}
        </nav>

        {/* User info + sign out, pinned to the foot of the sidebar instead of
            an otherwise-empty top bar. */}
        <div className="mt-4 border-t border-parchment-line pl-4 pr-5 pt-4">
          <p className="truncate text-sm font-medium text-ink">{user?.email}</p>
          <p className="font-mono text-xs uppercase tracking-wide text-slate">{user?.role}</p>
          <motion.button
            onClick={logout}
            whileHover={{ borderColor: 'var(--color-brass)' }}
            whileTap={{ scale: 0.96 }}
            className="mt-3 w-full rounded-md border border-parchment-line py-1.5 text-sm text-slate-dim hover:text-ink"
          >
            Sign out
          </motion.button>
        </div>
      </aside>

      <div className="flex flex-1 flex-col md:ml-56">
        <main className="flex-1 px-8 pb-10 pt-20 md:pt-10">
          <div className="page-shell rounded-md border border-parchment-line bg-white/60 p-8">
            <AnimatePresence mode="wait">
              <PageIn key={location.pathname}>{children}</PageIn>
            </AnimatePresence>
          </div>
        </main>
      </div>
    </div>
  )
}