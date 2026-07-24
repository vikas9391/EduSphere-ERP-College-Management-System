import { api } from './axios'
import { unwrapList, type PagedResponse } from './types'

export interface Department {
  id: number
  code: string
  name: string
  hod?: string
  description?: string
}

export interface DepartmentPayload {
  code: string
  name: string
  hod?: string
  description?: string
}

export async function getDepartments(): Promise<Department[]> {
  const res = await api.get<Department[] | PagedResponse<Department>>('/departments')
  return unwrapList(res.data)
}

export async function getDepartment(id: number): Promise<Department> {
  const res = await api.get<Department>(`/departments/${id}`)
  return res.data
}

export async function createDepartment(payload: DepartmentPayload): Promise<Department> {
  const res = await api.post<Department>('/departments', payload)
  return res.data
}

export async function updateDepartment(id: number, payload: DepartmentPayload): Promise<Department> {
  const res = await api.put<Department>(`/departments/${id}`, payload)
  return res.data
}

export async function deleteDepartment(id: number): Promise<void> {
  await api.delete(`/departments/${id}`)
}
