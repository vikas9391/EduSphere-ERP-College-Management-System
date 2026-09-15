import { api } from './axios'

export type TimetableDay = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export interface TimetableEntry {
  id: number
  classSubjectId: number
  schoolClassId: number
  schoolClassName: string
  academicYear: string
  semester: number
  subjectId: number | null
  subjectCode: string
  subjectName: string
  teacherId: number | null
  teacherName: string | null
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

export interface TimetableImportCandidate {
  classSubjectId: number
  className: string
  subjectName: string
  teacherName: string | null
  dayOfWeek: TimetableDay
  startTime: string
  endTime: string
  room: string | null
  confidence: number | null
}

export interface TimetableImportResponse {
  entries: TimetableImportCandidate[]
  warnings: string[]
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

export async function inspectTimetableImport(file: File): Promise<TimetableImportResponse> {
  const form = new FormData()
  form.append('file', file)
  const res = await api.post<TimetableImportResponse>('/timetable/import/inspect', form)
  return res.data
}
