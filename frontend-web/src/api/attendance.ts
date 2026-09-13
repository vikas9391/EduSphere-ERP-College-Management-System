import { api } from './axios'

export interface Attendance {
  id: number
  classEnrollmentId: number
  studentId: number
  studentName: string
  subjectId: number
  subjectName: string
  attendanceDate: string
  status: string
  remarks?: string
}

export interface AttendancePayload {
  classEnrollmentId: number
  attendanceDate: string
  status: string
  remarks?: string
}

export async function getAttendance(): Promise<Attendance[]> {
  const res = await api.get<Attendance[]>('/attendance')
  return res.data
}

export async function getStudentAttendance(studentId: number): Promise<Attendance[]> {
  const res = await api.get<Attendance[]>(`/attendance/student/${studentId}`)
  return res.data
}

export async function getMyAttendance(): Promise<Attendance[]> {
  const res = await api.get<Attendance[]>('/attendance/me')
  return res.data
}

export async function getAttendanceRecord(id: number): Promise<Attendance> {
  const res = await api.get<Attendance>(`/attendance/${id}`)
  return res.data
}

export async function createAttendance(payload: AttendancePayload): Promise<Attendance> {
  const res = await api.post<Attendance>('/attendance', payload)
  return res.data
}

export async function updateAttendance(id: number, payload: AttendancePayload): Promise<Attendance> {
  const res = await api.put<Attendance>(`/attendance/${id}`, payload)
  return res.data
}

export async function deleteAttendance(id: number): Promise<void> {
  await api.delete(`/attendance/${id}`)
}

export interface ClassHoliday {
  id: number
  classId: number
  className: string
  holidayDate: string
  reason?: string
}

export async function getClassHolidays(classId: number): Promise<ClassHoliday[]> {
  const res = await api.get<ClassHoliday[]>('/attendance/holidays', { params: { classId } })
  return res.data
}

export async function createClassHoliday(payload: { classId: number; holidayDate: string; reason?: string }): Promise<ClassHoliday> {
  const res = await api.post<ClassHoliday>('/attendance/holidays', payload)
  return res.data
}

export async function deleteClassHoliday(id: number): Promise<void> {
  await api.delete(`/attendance/holidays/${id}`)
}

export interface SubjectAttendanceSummary {
  subjectId: number | null
  subjectCode: string
  subjectName: string
  totalClasses: number
  classesAttended: number
  classesMissed: number
  attendancePercentage: number
}

export interface StudentAttendanceSummary {
  totalClasses: number
  classesAttended: number
  classesMissed: number
  overallAttendancePercentage: number
  bySubject: SubjectAttendanceSummary[]
}

export async function getMyAttendanceSummary(): Promise<StudentAttendanceSummary> {
  const res = await api.get<StudentAttendanceSummary>('/attendance/me/summary')
  return res.data
}
