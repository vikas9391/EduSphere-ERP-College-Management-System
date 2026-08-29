import { api } from './axios'

export interface Attendance {
  id: number
  enrollmentId: number | null
  studentId: number
  studentName: string
  subjectId: number | null
  subjectName: string
  classEnrollmentId?: number | null
  attendanceDate: string
  status: string
  remarks?: string
}
export interface AttendancePayload {
  enrollmentId?: number | null
  classEnrollmentId?: number | null
  attendanceDate: string
  status: string
  remarks?: string
}
export async function getAttendance(): Promise<Attendance[]> {
  const res = await api.get<Attendance[]>('/attendance'); return res.data
}
export async function getStudentAttendance(studentId: number): Promise<Attendance[]> {
  const res = await api.get<Attendance[]>(`/attendance/student/${studentId}`); return res.data
}
export async function getMyAttendance(): Promise<Attendance[]> {
  const res = await api.get<Attendance[]>('/attendance/me'); return res.data
}
export async function getAttendanceRecord(id: number): Promise<Attendance> {
  const res = await api.get<Attendance>(`/attendance/${id}`); return res.data
}
export async function createAttendance(payload: AttendancePayload): Promise<Attendance> {
  const res = await api.post<Attendance>('/attendance', payload); return res.data
}
export async function updateAttendance(id: number, payload: AttendancePayload): Promise<Attendance> {
  const res = await api.put<Attendance>(`/attendance/${id}`, payload); return res.data
}
export async function deleteAttendance(id: number): Promise<void> {
  await api.delete(`/attendance/${id}`)
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
  const records = await getMyAttendance()
  const attended = records.filter((r) => {
    const status = (r.status || '').trim().toUpperCase()
    return status === 'PRESENT' || status === 'ATTENDED'
  }).length

  const bySubject = Array.from(
    records.reduce((map, record) => {
      const key = String(record.subjectId ?? record.subjectName ?? 'unknown')
      const existing = map.get(key) ?? {
        subjectId: record.subjectId,
        subjectCode: '',
        subjectName: record.subjectName || 'Unknown Subject',
        totalClasses: 0,
        classesAttended: 0,
        classesMissed: 0,
        attendancePercentage: 0,
      }
      existing.totalClasses += 1
      if (['PRESENT', 'ATTENDED'].includes((record.status || '').trim().toUpperCase())) {
        existing.classesAttended += 1
      } else {
        existing.classesMissed += 1
      }
      map.set(key, existing)
      return map
    }, new Map<string, SubjectAttendanceSummary>())
  ).map((s) => ({
    ...s,
    attendancePercentage: s.totalClasses ? Number(((s.classesAttended / s.totalClasses) * 100).toFixed(1)) : 0,
  }))

  return {
    totalClasses: records.length,
    classesAttended: attended,
    classesMissed: records.length - attended,
    overallAttendancePercentage: records.length
      ? Number(((attended / records.length) * 100).toFixed(1))
      : 0,
    bySubject,
  }
}
