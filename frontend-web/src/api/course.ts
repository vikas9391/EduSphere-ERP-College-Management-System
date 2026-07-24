import { api } from './axios'
import { unwrapList, type PagedResponse } from './types'

/**
 * Mirrors com.collegeerp.Backend.course.dto.CourseResponse: a flat departmentId/
 * departmentName pair, NOT a nested Department object (the backend's CourseService
 * maps the JPA entity's `department` relation down to these two fields).
 */
export interface Course {
  id: number
  courseCode: string
  courseName: string
  duration: number
  description?: string
  departmentId: number
  departmentName: string
}

/**
 * Mirrors com.collegeerp.Backend.course.dto.CourseRequest exactly - `duration` is
 * @NotNull on the backend (required), `description` is optional.
 */
export interface CoursePayload {
  courseCode: string
  courseName: string
  duration: number
  description?: string
  departmentId: number
}

export async function getCourses(): Promise<Course[]> {
  const res = await api.get<Course[] | PagedResponse<Course>>('/courses')
  return unwrapList(res.data)
}

export async function getCourse(id: number): Promise<Course> {
  const res = await api.get<Course>(`/courses/${id}`)
  return res.data
}

export async function createCourse(payload: CoursePayload): Promise<Course> {
  const res = await api.post<Course>('/courses', payload)
  return res.data
}

export async function updateCourse(id: number, payload: CoursePayload): Promise<Course> {
  const res = await api.put<Course>(`/courses/${id}`, payload)
  return res.data
}

export async function deleteCourse(id: number): Promise<void> {
  await api.delete(`/courses/${id}`)
}
