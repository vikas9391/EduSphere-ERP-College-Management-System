import { api } from './axios'

export interface StudentDashboardSummary {
  studentId: number
  studentName: string
  rollNumber: string | null
  department: string | null
  course: string | null
  semester: number | null
  cgpa: number
  attendancePercentage: number
  totalSubjects: number
  pendingAssignments: number
  upcomingExams: number
  notificationsCount: number
}
export async function getStudentDashboardSummary(): Promise<StudentDashboardSummary> { const res = await api.get<StudentDashboardSummary>('/student/dashboard'); return res.data }

export interface MyAssignment {
  assignmentId: number
  title: string
  description: string
  subjectId: number
  subjectName: string
  teacherName: string
  dueDate: string
  maxMarks: number
  submissionStatus: string
  submittedAt: string | null
  submissionUrl: string | null
  marksObtained: number | null
  feedback: string | null
}
export async function getMyAssignments(): Promise<MyAssignment[]> { const res = await api.get<MyAssignment[]>('/student/assignments'); return res.data }

export interface MySubjectResult { subjectId: number; subjectCode: string; subjectName: string; credits: number; internalMarks: number; externalMarks: number; totalMarks: number; maxMarks: number; grade: string; gradePoint: number }
export interface MySemesterResult { studentId: number; studentName: string; semester: number; academicYear: string; subjects: MySubjectResult[]; totalCredits: number; sgpa: number; result: string }
export interface MyOverallResult { studentId: number; studentName: string; semesterResults: MySemesterResult[]; totalCredits: number; cgpa: number; overallResult: string }
export async function getMyResults(): Promise<MyOverallResult> { const res = await api.get<MyOverallResult>('/student/results'); return res.data }

export interface TimetableEntry { startTime: string; endTime: string; subjectId: number; subjectName: string; teacherName: string; room: string }
export interface StudentTimetable { placeholder: boolean; note: string; schedule: Record<string, TimetableEntry[]> }
export async function getMyTimetable(): Promise<StudentTimetable> { const res = await api.get<StudentTimetable>('/student/timetable'); return res.data }

export interface MyNotification { id: number; title: string; message: string; type: string; read: boolean; createdAt: string }

/** Real announcements now back the student dashboard notification section. */
export async function getMyNotifications(): Promise<MyNotification[]> {
  const res = await api.get<Array<{ id: number; title: string; message: string; createdAt: string; read: boolean }>>('/announcements')
  return res.data.map((a) => ({ id: a.id, title: a.title, message: a.message, type: 'ANNOUNCEMENT', read: a.read, createdAt: a.createdAt }))
}
