import { api } from './axios'
import type { Subject } from './subject'
import type { Enrollment } from './enrollment'

/* ------------------------------------------------------------------ */
/* Dashboard — mirrors com.collegeerp.Backend.teacher.dto.TeacherDashboardResponse */
/* One call replaces the previous approach of fetching every assignment/         */
/* submission/attendance/subject/enrollment row in the system and filtering      */
/* client-side by teacherId - see the backend's TeacherDashboardService.         */
/* ------------------------------------------------------------------ */

export interface SubjectAssignmentCount {
  subjectName: string
  count: number
}

export interface AttendanceTrendPoint {
  label: string
  ratePercentage: number
}

export interface TeacherDashboardAssignment {
  assignmentId: number
  title: string
  subjectId: number
  subjectName: string
  dueDate: string
  maxMarks: number
  totalSubmissions?: number
  evaluatedCount?: number
  pendingReviewCount?: number
}

/**
 * PLACEHOLDER on the backend for todaysSchedule/announcements: no Timetable or
 * Announcement entity exists in the schema yet. Subjects are real; day/time/room
 * assignments and announcement content are not read from any real schedule or
 * announcements system. See the backend's TeacherScheduleService /
 * TeacherAnnouncementService for the full explanation. `schedulePlaceholder` /
 * `announcementsPlaceholder` are how the backend flags this - surface it in the
 * UI, don't hide it.
 */
export interface TeacherScheduleEntry {
  subjectId: number
  subjectName: string
  startTime: string
  endTime: string
  room: string
}

export interface TeacherAnnouncement {
  id: number
  title: string
  body: string
  createdAt: string
}

export interface TeacherDashboardSummary {
  teacherId: number
  teacherName: string
  totalSubjects: number
  totalStudents: number
  pendingReviewCount: number
  attendancePendingToday: number
  upcomingClassesCount: number
  assignmentsPerSubject: SubjectAssignmentCount[]
  attendanceTrend: AttendanceTrendPoint[]
  recentAssignments: TeacherDashboardAssignment[]
  todaysSchedule: TeacherScheduleEntry[]
  schedulePlaceholder: boolean
  announcements: TeacherAnnouncement[]
  announcementsPlaceholder: boolean
}

export async function getTeacherDashboardSummary(): Promise<TeacherDashboardSummary> {
  const res = await api.get<TeacherDashboardSummary>('/teacher/dashboard')
  return res.data
}

/* ------------------------------------------------------------------ */
/* Subjects taught — mirrors SubjectResponse, scoped to the logged-in teacher */
/* ------------------------------------------------------------------ */

export async function getMySubjects(): Promise<Subject[]> {
  const res = await api.get<Subject[]>('/teacher/subjects')
  return res.data
}

/* ------------------------------------------------------------------ */
/* Roster — mirrors EnrollmentResponse, scoped to the logged-in teacher's subjects */
/* One row per (student, subject) pair.                                            */
/* ------------------------------------------------------------------ */

export async function getMyStudents(): Promise<Enrollment[]> {
  const res = await api.get<Enrollment[]>('/teacher/students')
  return res.data
}

/* ------------------------------------------------------------------ */
/* Assignments owned — mirrors TeacherAssignmentResponse, with submission     */
/* counts computed server-side instead of downloading every submission.      */
/* ------------------------------------------------------------------ */

export interface MyOwnedAssignment {
  assignmentId: number
  title: string
  subjectId: number
  subjectName: string
  dueDate: string
  maxMarks: number
  totalSubmissions: number
  evaluatedCount: number
  pendingReviewCount: number
}

export async function getMyOwnedAssignments(): Promise<MyOwnedAssignment[]> {
  const res = await api.get<MyOwnedAssignment[]>('/teacher/assignments')
  return res.data
}
