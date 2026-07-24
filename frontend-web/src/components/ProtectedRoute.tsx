import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'

/** Mirrors routeForRole() in LoginPage — kept in sync manually since there's no shared roles module yet. */
function dashboardForRole(userRole: string | undefined) {
  switch (userRole) {
    case 'SUPER_ADMIN':
      return '/colleges'
    case 'TEACHER':
      return '/teacher/dashboard'
    case 'STUDENT':
      return '/student/dashboard'
    case 'ADMIN':
      return '/admin/dashboard'
    default:
      return '/dashboard'
  }
}

/**
 * `role` restricts the route to a specific JWT role (e.g. "SUPER_ADMIN"). Signed-in
 * users of any other role are bounced to their own dashboard rather than to /login,
 * since they do have a valid session — they just don't have access to this page.
 */
export function ProtectedRoute({ children, role }: { children: ReactNode; role?: string }) {
  const token = useAuthStore((s) => s.token)
  const userRole = useAuthStore((s) => s.user?.role)

  if (!token) {
    return <Navigate to={role === 'SUPER_ADMIN' ? '/super-admin/login' : '/login'} replace />
  }

  if (role && userRole !== role) {
    return <Navigate to={dashboardForRole(userRole)} replace />
  }

  return <>{children}</>
}
