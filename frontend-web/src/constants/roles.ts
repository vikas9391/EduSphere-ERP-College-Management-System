export const ROLES = {
  SUPER_ADMIN: 'SUPER_ADMIN',
  ADMIN: 'ADMIN',
  TEACHER: 'TEACHER',
  STUDENT: 'STUDENT',
} as const

export type Role = (typeof ROLES)[keyof typeof ROLES]

/** Normalize only legacy spellings received from older tokens/data. */
export function normalizeRole(role: string | undefined | null): string {
  const value = (role ?? '').trim().toUpperCase()
  if (value.startsWith('ROLE_')) return normalizeRole(value.slice(5))
  if (value === 'STUDENTS') return ROLES.STUDENT
  if (value === 'TEACHERS') return ROLES.TEACHER
  if (value === 'ADMINS' || value === 'ADMINISTRATORS') return ROLES.ADMIN
  return value
}

export function isRole(role: string | undefined | null, expected: Role): boolean {
  return normalizeRole(role) === expected
}

export function dashboardForRole(role: string | undefined | null): string {
  switch (normalizeRole(role)) {
    case ROLES.SUPER_ADMIN: return '/colleges'
    case ROLES.ADMIN: return '/admin/dashboard'
    case ROLES.TEACHER: return '/teacher/dashboard'
    case ROLES.STUDENT: return '/student/dashboard'
    default: return '/dashboard'
  }
}
