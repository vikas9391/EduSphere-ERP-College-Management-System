import { api } from './axios'

/**
 * Mirrors com.collegeerp.Backend.common.dto.RoleResponse.
 */
export interface Role {
  id: number
  name: string
  description?: string
  isSystemRole: boolean
  permissions: string[]
}

/**
 * Mirrors com.collegeerp.Backend.common.dto.RoleRequest.
 */
export interface RolePayload {
  name: string
  description?: string
  permissions: string[]
}

/**
 * Mirrors com.collegeerp.Backend.common.dto.PermissionInfo — one entry per value of
 * the backend's Permission enum, grouped by category for the role-builder checklist.
 */
export interface PermissionInfo {
  name: string
  category: string
}

export async function getRoles(): Promise<Role[]> {
  const res = await api.get<Role[]>('/roles')
  return res.data
}

export async function getRole(id: number): Promise<Role> {
  const res = await api.get<Role>(`/roles/${id}`)
  return res.data
}

export async function createRole(payload: RolePayload): Promise<Role> {
  const res = await api.post<Role>('/roles', payload)
  return res.data
}

export async function updateRole(id: number, payload: RolePayload): Promise<Role> {
  const res = await api.put<Role>(`/roles/${id}`, payload)
  return res.data
}

export async function deleteRole(id: number): Promise<void> {
  await api.delete(`/roles/${id}`)
}

export async function getAllPermissions(): Promise<PermissionInfo[]> {
  const res = await api.get<PermissionInfo[]>('/roles/permissions')
  return res.data
}

/** Groups a flat permission list by category, in the order categories first appear. */
export function groupPermissionsByCategory(permissions: PermissionInfo[]): Map<string, PermissionInfo[]> {
  const groups = new Map<string, PermissionInfo[]>()
  for (const permission of permissions) {
    const existing = groups.get(permission.category)
    if (existing) existing.push(permission)
    else groups.set(permission.category, [permission])
  }
  return groups
}
