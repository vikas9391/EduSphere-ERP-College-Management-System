import { api } from './axios'
import type { Subject } from './subject'
import type { ClassEnrollment } from './schoolClass'

export interface SubjectAssignmentCount { subjectName: string; count: number }
export interface AttendanceTrendPoint { label: string; ratePercentage: number }
export interface TeacherDashboardAssignment { assignmentId: number; title: string; subjectId: number; subjectName: string; dueDate: string; maxMarks: number; totalSubmissions?: number; evaluatedCount?: number; pendingReviewCount?: number }
export interface TeacherScheduleEntry { subjectId: number; subjectName: string; startTime: string; endTime: string; room: string }
export interface TeacherAnnouncement { id: number; title: string; body: string; createdAt: string }
export interface TeacherDashboardSummary {
  teacherId: number; teacherName: string; totalSubjects: number; totalStudents: number; pendingReviewCount: number; attendancePendingToday: number; upcomingClassesCount: number
  assignmentsPerSubject: SubjectAssignmentCount[]; attendanceTrend: AttendanceTrendPoint[]; recentAssignments: TeacherDashboardAssignment[]; todaysSchedule: TeacherScheduleEntry[]
  schedulePlaceholder: boolean; announcements: TeacherAnnouncement[]; announcementsPlaceholder: boolean
}
export async function getTeacherDashboardSummary(): Promise<TeacherDashboardSummary> { const res = await api.get<TeacherDashboardSummary>('/teacher/dashboard'); return res.data }

export async function getMySubjects(): Promise<Subject[]> { const res = await api.get<Subject[]>('/teacher/subjects'); return res.data }

/** Teacher roster is class-scoped: one row per ClassEnrollment. */
export async function getMyStudents(): Promise<ClassEnrollment[]> { const res = await api.get<ClassEnrollment[]>('/teacher/students'); return res.data }

export interface MyOwnedAssignment { assignmentId: number; title: string; subjectId: number; subjectName: string; dueDate: string; maxMarks: number; totalSubmissions: number; evaluatedCount: number; pendingReviewCount: number }
export async function getMyOwnedAssignments(): Promise<MyOwnedAssignment[]> { const res = await api.get<MyOwnedAssignment[]>('/teacher/assignments'); return res.data }
