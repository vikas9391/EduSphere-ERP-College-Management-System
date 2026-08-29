import { api } from './axios'
import { unwrapList, type PagedResponse } from './types'

/**
 * Mirrors com.collegeerp.Backend.enrollment.dto.EnrollmentResponse exactly.
 */
export interface Enrollment {
  id: number
  studentId: number
  studentName: string
  admissionNo: string
  subjectId: number
  subjectName: string
  subjectCode: string
  courseName: string
  teacherName: string
  academicYear: string
  semester: number
  enrollmentDate: string
  status: string
}

export interface EnrollmentPayload {
  studentId: number
  subjectId: number
  academicYear: string
  semester: number
  enrollmentDate: string
}

export async function getEnrollments(): Promise<Enrollment[]> {
  const res = await api.get<Enrollment[] | PagedResponse<Enrollment>>('/enrollments')
  return unwrapList(res.data)
}

export async function getEnrollment(id: number): Promise<Enrollment> {
  const res = await api.get<Enrollment>(`/enrollments/${id}`)
  return res.data
}

export async function createEnrollment(payload: EnrollmentPayload): Promise<Enrollment> {
  const res = await api.post<Enrollment>('/enrollments', payload)
  return res.data
}

export async function updateEnrollment(id: number, payload: EnrollmentPayload): Promise<Enrollment> {
  const res = await api.put<Enrollment>(`/enrollments/${id}`, payload)
  return res.data
}

export async function deleteEnrollment(id: number): Promise<void> {
  await api.delete(`/enrollments/${id}`)
}

/** Student's own enrollments, resolved from the authenticated student's profile. */
export async function getMyEnrollments(): Promise<Enrollment[]> {
  const res = await api.get<Enrollment[]>('/enrollments/me')
  return res.data
}
