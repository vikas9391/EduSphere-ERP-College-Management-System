import { api } from './axios'

/**
 * Only the student dashboard has a real backend endpoint
 * (GET /api/student/dashboard, built during the Student Module work).
 *
 * getTeacherDashboard()/getAdminDashboard() previously called GET /api/teacher/dashboard
 * and GET /api/admin/dashboard - neither exists on the backend and both would 404.
 * They were also never actually called anywhere in the app: TeacherDashboard.tsx
 * builds its view by aggregating real data from the existing assignments/
 * submissions/attendance/subjects/enrollments endpoints and filtering by the
 * logged-in teacher's id client-side, and AdminDashboard (in DashboardPage.tsx) is
 * currently a static module-navigation page with placeholder stat cards. Removed
 * the dead functions rather than leave them pointing at endpoints that don't exist -
 * see README_PROGRESS.md/FRONTEND_PROGRESS.md for the AdminDashboard placeholder-stats
 * gap, which is a real but separate follow-up item.
 */
export async function getStudentDashboard() {
  const res = await api.get('/student/dashboard')
  return res.data
}
