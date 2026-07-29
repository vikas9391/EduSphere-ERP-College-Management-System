import { api } from './axios'

/**
 * Mirrors com.collegeerp.Backend.auth.LoginRequest exactly.
 */
export interface LoginPayload {
  collegeCode: string
  email: string
  password: string
}

/**
 * Mirrors com.collegeerp.Backend.auth.dto.LoginResponse exactly.
 * There is only ONE login endpoint on the backend (POST /api/auth/login) -
 * AuthController tries the staff/admin user table first, then the student
 * table, and returns whichever one matches. There is no per-role endpoint
 * (no /teacher/login, no /student/login) - the backend determines the role
 * from whichever account matched and returns it in this response.
 */
export interface LoginResponse {
  accessToken: string
  /** Long-lived (7 day) token used to silently renew accessToken once it expires,
   *  via POST /auth/refresh - see api/axios.ts's response interceptor. */
  refreshToken: string
  tokenType: string
  expiresInMillis: number
  email: string
  role: string
  tenantSchema: string
  /** True for a staff/admin account whose password was set by an admin and hasn't
   *  been changed yet - the frontend should route to /change-password before the
   *  dashboard. Always false for teacher/student/super-admin logins. */
  mustChangePassword: boolean
}

export async function login(payload: LoginPayload): Promise<LoginResponse> {
  const res = await api.post<LoginResponse>('/auth/login', payload)
  return res.data
}

/**
 * Mirrors com.collegeerp.Backend.auth.RefreshTokenRequest / the /api/auth/refresh
 * response (same LoginResponse shape as login - a fresh access token comes back
 * alongside a rotated refresh token). Called by the axios response interceptor, not
 * directly by page components.
 */
export async function refreshAccessToken(refreshToken: string): Promise<LoginResponse> {
  const res = await api.post<LoginResponse>('/auth/refresh', { refreshToken })
  return res.data
}

/**
 * Mirrors com.collegeerp.Backend.auth.SuperAdminLoginRequest.
 * No collegeCode — a super admin isn't scoped to any college.
 */
export interface SuperAdminLoginPayload {
  email: string
  password: string
}

/**
 * Backend route: POST /api/auth/super-admin/login. Returns the same LoginResponse
 * shape as the regular login, with role="SUPER_ADMIN" and tenantSchema="public".
 */
export async function superAdminLogin(payload: SuperAdminLoginPayload): Promise<LoginResponse> {
  const res = await api.post<LoginResponse>('/auth/super-admin/login', payload)
  return res.data
}

/**
 * Mirrors com.collegeerp.Backend.auth.ForgotPasswordRequest. collegeCode follows the
 * same convention as LoginPayload — the reserved super-admin code routes to the
 * public-schema super admin flow instead of a tenant.
 */
export interface ForgotPasswordPayload {
  collegeCode: string
  email: string
}

/**
 * Backend route: POST /api/auth/forgot-password. Always resolves (the backend
 * returns the same generic success message whether or not the email matched an
 * account, to avoid leaking which emails are registered) — the caller should just
 * show a "check your email" message, not branch on the response.
 */
export async function forgotPassword(payload: ForgotPasswordPayload): Promise<void> {
  await api.post('/auth/forgot-password', payload)
}

/**
 * Mirrors com.collegeerp.Backend.auth.ResetPasswordRequest. collegeCode and token
 * both arrive as query params on the emailed reset link (see
 * PasswordResetService#requestReset) and are threaded through unchanged.
 */
export interface ResetPasswordPayload {
  collegeCode: string
  token: string
  newPassword: string
}

/** Backend route: POST /api/auth/reset-password. */
export async function resetPassword(payload: ResetPasswordPayload): Promise<void> {
  await api.post('/auth/reset-password', payload)
}
