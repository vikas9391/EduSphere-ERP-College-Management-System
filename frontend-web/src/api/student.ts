import { api } from './axios'
import { unwrapList, type PagedResponse } from './types'

/**
 * Mirrors com.collegeerp.Backend.student.dto.StudentResponse. Only admissionNo,
 * firstName, and email are guaranteed non-null on the backend - everything else
 * is a nullable column, so those fields are optional here (previously this
 * interface marked them all as required strings, which didn't match what the
 * API can actually return and would have hidden real null values behind a
 * false type guarantee).
 */
export interface Student {
  id: number
  admissionNo: string
  rollNumber?: string
  firstName: string
  lastName?: string
  email: string
  phone?: string
  gender?: string
  dateOfBirth?: string
  admissionDate?: string
  /** The course/programme this student is admitted into. Both null when unassigned. */
  courseId?: number
  courseName?: string
  address?: string
  city?: string
  state?: string
  country?: string
  pincode?: string
  fatherName?: string
  motherName?: string
  parentPhone?: string
  parentEmail?: string
  bloodGroup?: string
  category?: string
  nationality?: string
  photoUrl?: string
  status?: string
}

/**
 * Mirrors com.collegeerp.Backend.student.dto.StudentRequest exactly. Only
 * admissionNo, firstName, and email are @NotBlank on the backend; `password`
 * is required when creating a student but optional when updating one (a blank
 * password on update leaves the existing password unchanged - enforced
 * server-side in StudentService). Everything else is genuinely optional.
 */
export interface StudentPayload {
  admissionNo: string
  rollNumber?: string
  password?: string
  firstName: string
  lastName?: string
  email: string
  phone?: string
  gender?: string
  dateOfBirth?: string
  admissionDate?: string
  /** Sending undefined/null clears the student's course assignment on update. */
  courseId?: number
  address?: string
  city?: string
  state?: string
  country?: string
  pincode?: string
  fatherName?: string
  motherName?: string
  parentPhone?: string
  parentEmail?: string
  bloodGroup?: string
  category?: string
  nationality?: string
  aadhaarNumber?: string
  photoUrl?: string
}

export async function getStudents(): Promise<Student[]> {
  const res = await api.get<Student[] | PagedResponse<Student>>('/students')
  return unwrapList(res.data)
}

export async function getStudent(id: number): Promise<Student> {
  const res = await api.get<Student>(`/students/${id}`)
  return res.data
}

export async function createStudent(payload: StudentPayload): Promise<Student> {
  const res = await api.post<Student>('/students', payload)
  return res.data
}

export async function updateStudent(id: number, payload: StudentPayload): Promise<Student> {
  const res = await api.put<Student>(`/students/${id}`, payload)
  return res.data
}

export async function deleteStudent(id: number): Promise<void> {
  await api.delete(`/students/${id}`)
}
