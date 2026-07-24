import { api } from './axios'

/* ------------------------------------------------------------------ */
/* Dashboard — mirrors com.collegeerp.Backend.student.dto.StudentDashboardResponse */
/* ------------------------------------------------------------------ */

export interface StudentDashboardSummary {
  studentId: number
  studentName: string
  rollNumber: string | null
  /** Derived from the student's most recent enrollment; null if not enrolled in anything yet. */
  department: string | null
  course: string | null
  semester: number | null
  /** 0 if the student has no PUBLISHED results yet. */
  cgpa: number
  /** 0 if the student has no attendance records yet. */
  attendancePercentage: number
  totalSubjects: number
  pendingAssignments: number
  upcomingExams: number
  notificationsCount: number
}

export async function getStudentDashboardSummary(): Promise<StudentDashboardSummary> {
  const res = await api.get<StudentDashboardSummary>('/student/dashboard')
  return res.data
}

/* ------------------------------------------------------------------ */
/* Attendance — mirrors StudentAttendanceResponse / SubjectAttendanceResponse */
/* ------------------------------------------------------------------ */

export interface SubjectAttendance {
  subjectId: number
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
  bySubject: SubjectAttendance[]
}

export async function getMyAttendanceSummary(): Promise<StudentAttendanceSummary> {
  const res = await api.get<StudentAttendanceSummary>('/student/attendance')
  return res.data
}

/* ------------------------------------------------------------------ */
/* Assignments — mirrors StudentAssignmentResponse */
/* ------------------------------------------------------------------ */

export interface MyAssignment {
  assignmentId: number
  title: string
  description: string
  subjectId: number
  subjectName: string
  teacherName: string
  dueDate: string
  maxMarks: number
  /** NOT_SUBMITTED, SUBMITTED, or the submission's own status (e.g. GRADED) once evaluated. */
  submissionStatus: string
  submittedAt: string | null
  /** Null until the teacher has graded the submission. */
  marksObtained: number | null
  feedback: string | null
}

export async function getMyAssignments(): Promise<MyAssignment[]> {
  const res = await api.get<MyAssignment[]>('/student/assignments')
  return res.data
}

/* ------------------------------------------------------------------ */
/* Results — mirrors OverallResultResponse / SemesterResultResponse / SubjectResultResponse */
/* ------------------------------------------------------------------ */

export interface MySubjectResult {
  subjectId: number
  subjectCode: string
  subjectName: string
  credits: number
  internalMarks: number
  externalMarks: number
  totalMarks: number
  maxMarks: number
  grade: string
  gradePoint: number
}

export interface MySemesterResult {
  studentId: number
  studentName: string
  semester: number
  academicYear: string
  subjects: MySubjectResult[]
  totalCredits: number
  sgpa: number
  result: string
}

export interface MyOverallResult {
  studentId: number
  studentName: string
  semesterResults: MySemesterResult[]
  totalCredits: number
  cgpa: number
  overallResult: string
}

export async function getMyResults(): Promise<MyOverallResult> {
  const res = await api.get<MyOverallResult>('/student/results')
  return res.data
}

/* ------------------------------------------------------------------ */
/* Timetable — mirrors StudentTimetableResponse / TimetableEntryResponse            */
/* PLACEHOLDER on the backend: no Timetable entity exists in the schema yet, so     */
/* day/time/room values are deterministically generated from the student's real    */
/* enrolled subjects, not read from any real scheduling data. See the backend's     */
/* StudentTimetableService for the full explanation. `placeholder`/`note` below     */
/* are how the backend flags this - surface them in the UI, don't hide them.        */
/* ------------------------------------------------------------------ */

export interface TimetableEntry {
  startTime: string
  endTime: string
  subjectId: number
  subjectName: string
  teacherName: string
  room: string
}

export interface StudentTimetable {
  placeholder: boolean
  note: string
  schedule: Record<string, TimetableEntry[]>
}

export async function getMyTimetable(): Promise<StudentTimetable> {
  const res = await api.get<StudentTimetable>('/student/timetable')
  return res.data
}

/* ------------------------------------------------------------------ */
/* Notifications — mirrors NotificationResponse                                     */
/* PLACEHOLDER on the backend: no Notification entity/table exists yet, this is     */
/* mock data. See the backend's StudentNotificationService for the full explanation. */
/* ------------------------------------------------------------------ */

export interface MyNotification {
  id: number
  title: string
  message: string
  type: string
  read: boolean
  createdAt: string
}

export async function getMyNotifications(): Promise<MyNotification[]> {
  const res = await api.get<MyNotification[]>('/student/notifications')
  return res.data
}