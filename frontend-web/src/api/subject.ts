import { api } from './axios'
import { unwrapList, type PagedResponse } from './types'

export interface Subject {
  id: number
  subjectCode: string
  subjectName: string
  credits: number
  semester: number
  courseId: number
  courseName: string
  teacherId: number
  teacherName: string
}

export interface SubjectPayload {
  subjectCode: string
  subjectName: string
  credits: number
  semester: number
  courseId: number
  teacherId: number
}

export async function getSubjects(): Promise<Subject[]> {
  const res = await api.get<Subject[] | PagedResponse<Subject>>('/subjects')
  return unwrapList(res.data)
}

export async function getSubject(id: number): Promise<Subject> {
  const res = await api.get<Subject>(`/subjects/${id}`)
  return res.data
}

export async function createSubject(payload: SubjectPayload): Promise<Subject> {
  const res = await api.post<Subject>('/subjects', payload)
  return res.data
}

export async function updateSubject(id: number, payload: SubjectPayload): Promise<Subject> {
  const res = await api.put<Subject>(`/subjects/${id}`, payload)
  return res.data
}

export async function deleteSubject(id: number): Promise<void> {
  await api.delete(`/subjects/${id}`)
}
