import { api } from './axios'
import { unwrapList, type PagedResponse } from './types'

/**
 * Mirrors com.collegeerp.Backend.common.dto.UserResponse.
 */
export interface StaffUser {
  id: number
  firstName: string
  lastName: string
  email: string
  roleId: number
  roleName: string
  isActive: boolean
  mustChangePassword: boolean
  createdAt: string
}

/**
 * Mirrors com.collegeerp.Backend.common.dto.UserCreateRequest. Generalized user
 * creation - any role can be assigned here (HOD, Supervisor, Accountant, Librarian,
 * or a custom role), not just Teacher accounts.
 */
export interface UserCreatePayload {
  firstName: string
  lastName: string
  email: string
  password: string
  roleId: number
}

export interface RoleAssignmentPayload {
  roleId: number
}

/**
 * Mirrors com.collegeerp.Backend.common.dto.PasswordChangeRequest.
 */
export interface PasswordChangePayload {
  currentPassword: string
  newPassword: string
}

export async function getUsers(): Promise<StaffUser[]> {
  const res = await api.get<StaffUser[] | PagedResponse<StaffUser>>('/users')
  return unwrapList(res.data)
}

export async function createUser(payload: UserCreatePayload): Promise<StaffUser> {
  const res = await api.post<StaffUser>('/users', payload)
  return res.data
}

/** Assign a new role to an existing staff account. Backend enforces ASSIGN_ROLE. */
export async function assignUserRole(id: number, payload: RoleAssignmentPayload): Promise<StaffUser> {
  const res = await api.put<StaffUser>(`/users/${id}/role`, payload)
  return res.data
}

export async function changeMyPassword(payload: PasswordChangePayload): Promise<void> {
  await api.put('/users/me/password', payload)
}
