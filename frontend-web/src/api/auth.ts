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
  tokenType: string
  expiresInMillis: number
  email: string
  role: string
  tenantSchema: string
}

export async function login(payload: LoginPayload): Promise<LoginResponse> {
  const res = await api.post<LoginResponse>('/auth/login', payload)
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
