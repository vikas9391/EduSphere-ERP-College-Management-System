import { api } from './axios'

export type EnrollmentMode = 'MANDATORY' | 'ELECTIVE'
export type EnrollmentSource = 'AUTO' | 'SELF'

export interface SchoolClass {
  id: number
  name: string
  academicYear: string
  semester: number
  maxSubjects: number | null
  teacherId: number
  teacherName: string
  studentCount: number
  subjectCount: number
}

export interface SchoolClassPayload {
  name: string
  academicYear: string
  semester: number
  maxSubjects: number | null
}

export interface ClassStudent {
  studentId: number
  admissionNo: string
  studentName: string
  addedAt: string
}

export interface ClassSubject {
  id: number
  schoolClassId: number
  subjectCode: string
  subjectName: string
  credits: number
  teacherId: number
  teacherName: string
  enrollmentMode: EnrollmentMode
  enrolledCount: number
  enrolledByMe?: boolean | null
}

export interface ClassSubjectPayload {
  subjectCode: string
  subjectName: string
  credits: number
  teacherId: number
  enrollmentMode: EnrollmentMode
}

export interface ClassEnrollment {
  id: number
  classSubjectId: number
  subjectCode: string
  subjectName: string
  studentId: number
  studentName: string
  source: EnrollmentSource
  enrolledAt: string
}

// ---- Classes (teacher-owned) ----

export async function getMyClasses(): Promise<SchoolClass[]> {
  const res = await api.get<SchoolClass[]>('/classes/mine')
  return res.data
}

export async function getMyClassesAsStudent(): Promise<SchoolClass[]> {
  const res = await api.get<SchoolClass[]>('/classes/mine-as-student')
  return res.data
}

export async function getSchoolClass(id: number): Promise<SchoolClass> {
  const res = await api.get<SchoolClass>(`/classes/${id}`)
  return res.data
}

export async function createSchoolClass(payload: SchoolClassPayload): Promise<SchoolClass> {
  const res = await api.post<SchoolClass>('/classes', payload)
  return res.data
}

export async function deleteSchoolClass(id: number): Promise<void> {
  await api.delete(`/classes/${id}`)
}

// ---- Roster ----

export async function getRoster(classId: number): Promise<ClassStudent[]> {
  const res = await api.get<ClassStudent[]>(`/classes/${classId}/students`)
  return res.data
}

export async function addStudentsToClass(classId: number, studentIds: number[]): Promise<ClassStudent[]> {
  const res = await api.post<ClassStudent[]>(`/classes/${classId}/students`, { studentIds })
  return res.data
}

export async function removeStudentFromClass(classId: number, studentId: number): Promise<void> {
  await api.delete(`/classes/${classId}/students/${studentId}`)
}

// ---- Subjects ----

export async function getClassSubjects(classId: number): Promise<ClassSubject[]> {
  const res = await api.get<ClassSubject[]>(`/classes/${classId}/subjects`)
  return res.data
}

export async function getClassSubjectsForStudent(classId: number): Promise<ClassSubject[]> {
  const res = await api.get<ClassSubject[]>(`/classes/${classId}/subjects/mine`)
  return res.data
}

export async function createClassSubject(classId: number, payload: ClassSubjectPayload): Promise<ClassSubject> {
  const res = await api.post<ClassSubject>(`/classes/${classId}/subjects`, payload)
  return res.data
}

export async function createClassSubjectsBulk(classId: number, payloads: ClassSubjectPayload[]): Promise<ClassSubject[]> {
  const res = await api.post<ClassSubject[]>(`/classes/${classId}/subjects/bulk`, payloads)
  return res.data
}

export async function deleteClassSubject(classId: number, subjectId: number): Promise<void> {
  await api.delete(`/classes/${classId}/subjects/${subjectId}`)
}

export async function getClassSubjectEnrollments(subjectId: number): Promise<ClassEnrollment[]> {
  const res = await api.get<ClassEnrollment[]>(`/classes/subjects/${subjectId}/enrollments`)
  return res.data
}

export async function selfEnrollInClassSubject(subjectId: number): Promise<ClassEnrollment> {
  const res = await api.post<ClassEnrollment>(`/classes/subjects/${subjectId}/enroll`)
  return res.data
}

export async function selfDropClassSubject(subjectId: number): Promise<void> {
  await api.delete(`/classes/subjects/${subjectId}/enroll`)
}
