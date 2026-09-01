import { useEffect, useState, type ReactNode } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { useAuthStore } from '@/store/authStore'
import { PageIn } from '@/components/motion'
import { LeafDivider } from '@/components/LeafDivider'
import { navModulesForRole } from '@/config/staffModules'
import { LayoutDashboard, Layers, Award, CalendarCheck, CalendarDays, ClipboardList, Upload, UserCircle, Building2, BookMarked, Users, Menu, X, Sprout, Megaphone } from 'lucide-react'

const superAdminNavItems = [{ to: '/colleges', label: 'Colleges', icon: Building2 }]
const teacherNavItems = [
  { to: '/teacher/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/teacher/classes', label: 'Classes', icon: Layers },
  { to: '/teacher/timetable', label: 'Timetable', icon: CalendarDays },
  { to: '/attendance', label: 'Attendance', icon: CalendarCheck },
  { to: '/assignments', label: 'Assignments', icon: ClipboardList },
  { to: '/submissions', label: 'Submissions', icon: Upload },
  { to: '/results', label: 'Results', icon: Award },
  { to: '/announcements', label: 'Announcements', icon: Megaphone },
]
const studentNavItems = [
  { to: '/student/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/student/profile', label: 'My Profile', icon: UserCircle },
  { to: '/student/classes', label: 'My Classes', icon: Users },
  { to: '/student/enrollments', label: 'My Enrollments', icon: BookMarked },
  { to: '/student/timetable', label: 'My Timetable', icon: CalendarDays },
  { to: '/student/attendance', label: 'My Attendance', icon: CalendarCheck },
  { to: '/student/assignments', label: 'My Assignments', icon: ClipboardList },
  { to: '/announcements', label: 'Announcements', icon: Megaphone },
]

function navItemsForRole(role: string | undefined, permissions: string[]) {
  switch (role) {
    case 'SUPER_ADMIN': return superAdminNavItems
    case 'TEACHER': return teacherNavItems
    case 'STUDENT': return studentNavItems
    default: return navModulesForRole(role, permissions).map(({ to, label, icon }) => ({ to, label, icon }))
  }
}

export function Layout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuthStore()
  const navItems = navItemsForRole(user?.role, user?.permissions ?? [])
  const location = useLocation()
  const [mobileNavOpen, setMobileNavOpen] = useState(false)
  useEffect(() => { setMobileNavOpen(false) }, [location.pathname])

  return (
    <div className="flex min-h-screen bg-bg font-body">
      <button onClick={() => setMobileNavOpen((open) => !open)} aria-label={mobileNavOpen ? 'Close menu' : 'Open menu'} aria-expanded={mobileNavOpen} className="fixed left-4 top-4 z-40 flex h-10 w-10 items-center justify-center rounded-2xl bg-card shadow-[var(--shadow-card)] md:hidden">
        {mobileNavOpen ? <X size={18} /> : <Menu size={18} />}
      </button>
      <AnimatePresence>{mobileNavOpen && <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} transition={{ duration: 0.2 }} onClick={() => setMobileNavOpen(false)} className="fixed inset-0 z-20 bg-text/40 md:hidden" />}</AnimatePresence>
      <aside className={`fixed inset-y-0 left-0 z-30 flex w-64 flex-col overflow-y-auto py-6 pl-3.5 pr-3.5 transition-transform duration-300 ease-in-out md:inset-y-4 md:left-4 md:h-[calc(100vh-2rem)] md:rounded-[var(--radius-card)] md:shadow-[var(--shadow-card)] md:translate-x-0 ${mobileNavOpen ? 'translate-x-0' : '-translate-x-full'}`} style={{ backgroundImage: 'linear-gradient(to top, var(--color-light-green) 0%, var(--color-card) 65%)' }}>
        <span className="mb-8 flex items-center gap-2.5 pl-3 font-heading text-lg font-semibold text-text">
          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary"><Sprout size={18} strokeWidth={2.2} className="text-white" /></span>
          <span className="leading-tight"><span className="block">EduSphere</span><span className="block font-body text-[11px] font-normal text-muted">Smart Campus, Smarter Future</span></span>
        </span>
        <nav className="flex flex-1 flex-col gap-1">
          {navItems.map(({ to, label, icon: Icon }) => {
            const isActive = to === location.pathname || (to !== '/dashboard' && to !== '/teacher/dashboard' && to !== '/student/dashboard' && location.pathname.startsWith(to))
            return <NavLink key={to} to={to} end={to === '/dashboard' || to === '/teacher/dashboard' || to === '/student/dashboard'} data-active={isActive} className="nav-pill flex items-center gap-2.5 px-3.5 py-2.5 text-sm text-muted hover:bg-hover data-[active=true]:font-semibold"><Icon size={16} />{label}</NavLink>
          })}
        </nav>
        <div className="mt-4 border-t border-border pl-3.5 pr-3 pt-4">
          <p className="truncate text-sm font-medium text-text">{user?.email}</p><p className="font-numbers text-xs uppercase tracking-wide text-muted">{user?.role}</p>
          <NavLink to="/change-password" className="mt-3 block text-xs text-muted hover:text-primary">Change password</NavLink>
          <motion.button onClick={logout} whileHover={{ backgroundColor: 'var(--color-hover)' }} whileTap={{ scale: 0.96 }} className="mt-3 w-full rounded-[var(--radius-btn)] border border-border py-1.5 text-sm text-muted hover:text-primary">Sign out</motion.button>
        </div>
      </aside>
      <div className="fixed inset-y-4 left-[256px] z-40 hidden md:block"><LeafDivider /></div>
      <div className="flex flex-1 flex-col md:ml-[304px]"><main className="flex-1 px-8 pb-10 pt-20 md:pt-10"><div className="rounded-[var(--radius-card)] bg-card p-8 shadow-[var(--shadow-card)]"><AnimatePresence mode="wait"><PageIn key={location.pathname}>{children}</PageIn></AnimatePresence></div></main></div>
    </div>
  )
}
