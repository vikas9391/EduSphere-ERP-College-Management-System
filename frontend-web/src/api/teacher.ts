import { api } from './axios'
import { unwrapList, type PagedResponse } from './types'

/**
 * Mirrors com.collegeerp.Backend.teacher.dto.TeacherResponse.
 */
export interface Teacher {
  id: number
  employeeId: string
  firstName: string
  lastName?: string
  email: string
  phone?: string
  gender?: string
  qualification?: string
  specialization?: string
  experience?: number
  joiningDate: string
}

/**
 * Mirrors com.collegeerp.Backend.teacher.dto.TeacherRequest exactly, including which
 * fields are required (@NotBlank/@NotNull on the backend: employeeId, firstName,
 * email, joiningDate) vs optional. `joiningDate` was previously missing from this
 * type entirely - every create/update would have failed backend validation with a
 * 400 ("Joining date is required") since the field could never be sent.
 *
 * `password` is required when creating a teacher but optional when updating one -
 * a blank password on update leaves the existing password unchanged (enforced
 * server-side in TeacherService), identical to the student password convention.
 */
export interface TeacherPayload {
  employeeId: string
  firstName: string
  lastName?: string
  email: string
  password?: string
  phone?: string
  gender?: string
  qualification?: string
  specialization?: string
  experience?: number
  joiningDate: string
}

export async function getTeachers(): Promise<Teacher[]> {
  const res = await api.get<Teacher[] | PagedResponse<Teacher>>('/teachers')
  return unwrapList(res.data)
}

export async function getTeacher(id: number): Promise<Teacher> {
  const res = await api.get<Teacher>(`/teachers/${id}`)
  return res.data
}

export async function createTeacher(payload: TeacherPayload): Promise<Teacher> {
  const res = await api.post<Teacher>('/teachers', payload)
  return res.data
}

export async function updateTeacher(id: number, payload: TeacherPayload): Promise<Teacher> {
  const res = await api.put<Teacher>(`/teachers/${id}`, payload)
  return res.data
}

export async function deleteTeacher(id: number): Promise<void> {
  await api.delete(`/teachers/${id}`)
}
