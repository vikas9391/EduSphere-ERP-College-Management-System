import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { dashboardForRole, isRole, ROLES, type Role } from '@/constants/roles'

/**
 * Protects authenticated frontend routes. Backend authorization remains the real
 * security boundary; these checks keep users out of pages that are clearly intended
 * for a different portal and avoid rendering unusable screens before an API call.
 */
export function ProtectedRoute({
  children,
  role,
  staffOnly = false,
}: {
  children: ReactNode
  role?: Role
  staffOnly?: boolean
}) {
  const token = useAuthStore((s) => s.token)
  const userRole = useAuthStore((s) => s.user?.role)

  if (!token) {
    return <Navigate to={isRole(role, ROLES.SUPER_ADMIN) ? '/super-admin/login' : '/login'} replace />
  }

  if (role && !isRole(userRole, role)) {
    return <Navigate to={dashboardForRole(userRole)} replace />
  }

  if (staffOnly && (isRole(userRole, ROLES.SUPER_ADMIN) || isRole(userRole, ROLES.TEACHER) || isRole(userRole, ROLES.STUDENT))) {
    return <Navigate to={dashboardForRole(userRole)} replace />
  }

  return <>{children}</>
}
