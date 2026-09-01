import { api } from './axios'

export type TimetableDay = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export interface TimetableEntry {
  id: number
  classSubjectId: number
  schoolClassId: number
  schoolClassName: string
  academicYear: string
  semester: number
  subjectId: number
  subjectCode: string
  subjectName: string
  teacherId: number
  teacherName: string
  dayOfWeek: TimetableDay
  startTime: string
  endTime: string
  room: string | null
}

export interface TimetableEntryPayload {
  classSubjectId: number
  dayOfWeek: TimetableDay
  startTime: string
  endTime: string
  room: string
}

export async function getMyTimetableEntries(): Promise<TimetableEntry[]> {
  const res = await api.get<TimetableEntry[]>('/timetable/mine')
  return res.data
}

export async function getClassSubjectTimetable(classSubjectId: number): Promise<TimetableEntry[]> {
  const res = await api.get<TimetableEntry[]>(`/timetable/class-subject/${classSubjectId}`)
  return res.data
}

export async function createTimetableEntry(payload: TimetableEntryPayload): Promise<TimetableEntry> {
  const res = await api.post<TimetableEntry>('/timetable', payload)
  return res.data
}

export async function updateTimetableEntry(id: number, payload: TimetableEntryPayload): Promise<TimetableEntry> {
  const res = await api.put<TimetableEntry>(`/timetable/${id}`, payload)
  return res.data
}

export async function deleteTimetableEntry(id: number): Promise<void> {
  await api.delete(`/timetable/${id}`)
}
