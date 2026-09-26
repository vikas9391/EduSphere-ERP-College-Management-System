// src/App.tsx

import { Suspense, lazy, useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import { ProtectedRoute } from "@/components/ProtectedRoute";
import { useAuthStore } from "@/store/authStore";
import { isRole, ROLES } from "@/constants/roles";
import { BookLoader, NavigationLoader } from "@/components/BookLoader";

import { LoginPage } from "@/pages/LoginPage";
import { ChangePasswordPage } from "@/pages/ChangePasswordPage";
import { ForgotPasswordPage } from "@/pages/ForgotPasswordPage";
import { ResetPasswordPage } from "@/pages/ResetPasswordPage";

const loadCollegesPage = () => import("@/pages/Collegespage").then((m) => ({ default: m.CollegesPage }));
const CollegesPage = lazy(loadCollegesPage);
const loadCollegeDetailPage = () => import("@/pages/CollegeDetailPage").then((m) => ({ default: m.CollegeDetailPage }));
const CollegeDetailPage = lazy(loadCollegeDetailPage);
const loadDashboardPage = () => import("@/pages/DashboardPage").then((m) => ({ default: m.DashboardPage }));
const DashboardPage = lazy(loadDashboardPage);
const loadAdminDashboard = () => import("@/pages/DashboardPage").then((m) => ({ default: m.AdminDashboard }));
const AdminDashboard = lazy(loadAdminDashboard);
const loadDepartmentsPage = () => import("@/pages/DepartmentsPage").then((m) => ({ default: m.DepartmentsPage }));
const DepartmentsPage = lazy(loadDepartmentsPage);
const loadCoursesPage = () => import("@/pages/CoursesPage").then((m) => ({ default: m.CoursesPage }));
const CoursesPage = lazy(loadCoursesPage);
const loadSubjectsPage = () => import("@/pages/SubjectsPage").then((m) => ({ default: m.SubjectsPage }));
const SubjectsPage = lazy(loadSubjectsPage);
const loadTeachersPage = () => import("@/pages/TeachersPage").then((m) => ({ default: m.TeachersPage }));
const TeachersPage = lazy(loadTeachersPage);
const loadStudentsPage = () => import("@/pages/student/StudentsPage").then((m) => ({ default: m.StudentsPage }));
const StudentsPage = lazy(loadStudentsPage);
const loadRolesPage = () => import("@/pages/RolesPage").then((m) => ({ default: m.RolesPage }));
const RolesPage = lazy(loadRolesPage);
const loadUsersPage = () => import("@/pages/UsersPage").then((m) => ({ default: m.UsersPage }));
const UsersPage = lazy(loadUsersPage);
const loadExamsPage = () => import("@/pages/ExamsPage").then((m) => ({ default: m.ExamsPage }));
const ExamsPage = lazy(loadExamsPage);
const loadExamSchedulePage = () => import("@/pages/ExamSchedulePage").then((m) => ({ default: m.ExamSchedulePage }));
const ExamSchedulePage = lazy(loadExamSchedulePage);
const loadMarksEntryPage = () => import("@/pages/MarksEntryPage").then((m) => ({ default: m.MarksEntryPage }));
const MarksEntryPage = lazy(loadMarksEntryPage);
const loadResultsPage = () => import("@/pages/ResultsPage").then((m) => ({ default: m.ResultsPage }));
const ResultsPage = lazy(loadResultsPage);
const loadAttendancePage = () => import("@/pages/AttendancePage").then((m) => ({ default: m.AttendancePage }));
const AttendancePage = lazy(loadAttendancePage);
const loadClassHolidaysPage = () => import("@/pages/ClassHolidaysPage").then((m) => ({ default: m.ClassHolidaysPage }));
const ClassHolidaysPage = lazy(loadClassHolidaysPage);
const loadAssignmentsPage = () => import("@/pages/AssignmentsPage").then((m) => ({ default: m.AssignmentsPage }));
const AssignmentsPage = lazy(loadAssignmentsPage);
const loadSubmissionsPage = () => import("@/pages/Submissionspage").then((m) => ({ default: m.SubmissionsPage }));
const SubmissionsPage = lazy(loadSubmissionsPage);
const loadAnnouncementsPage = () => import("@/pages/AnnouncementsPage").then((m) => ({ default: m.AnnouncementsPage }));
const AnnouncementsPage = lazy(loadAnnouncementsPage);
const loadTeacherDashboard = () => import("@/pages/Teacherdashboard").then((m) => ({ default: m.TeacherDashboard }));
const TeacherDashboard = lazy(loadTeacherDashboard);
const loadTeacherTimetablePage = () => import("@/pages/TeacherTimetablePage").then((m) => ({ default: m.TeacherTimetablePage }));
const TeacherTimetablePage = lazy(loadTeacherTimetablePage);
const loadClassesPage = () => import("@/pages/ClassesPage").then((m) => ({ default: m.ClassesPage }));
const ClassesPage = lazy(loadClassesPage);
const loadClassDetailPage = () => import("@/pages/ClassDetailPage").then((m) => ({ default: m.ClassDetailPage }));
const ClassDetailPage = lazy(loadClassDetailPage);
const loadStudentDashboard = () => import("@/pages/StudentDashboard").then((m) => ({ default: m.StudentDashboard }));
const StudentDashboard = lazy(loadStudentDashboard);
const loadStudentProfilePage = () => import("@/pages/student/StudentProfilePage").then((m) => ({ default: m.StudentProfilePage }));
const StudentProfilePage = lazy(loadStudentProfilePage);
const loadStudentEnrollmentsPage = () => import("@/pages/student/Studentenrollmentspage").then((m) => ({ default: m.StudentEnrollmentsPage }));
const StudentEnrollmentsPage = lazy(loadStudentEnrollmentsPage);
const loadStudentAssignmentsPage = () => import("@/pages/student/StudentAssignmentsPage").then((m) => ({ default: m.StudentAssignmentsPage }));
const StudentAssignmentsPage = lazy(loadStudentAssignmentsPage);
const loadStudentAttendancePage = () => import("@/pages/StudentAttendancePage.tsx").then((m) => ({ default: m.StudentAttendancePage }));
const StudentAttendancePage = lazy(loadStudentAttendancePage);
const loadStudentClassesPage = () => import("@/pages/student/StudentClassesPage").then((m) => ({ default: m.StudentClassesPage }));
const StudentClassesPage = lazy(loadStudentClassesPage);
const loadStudentTimetablePage = () => import("@/pages/student/StudentTimetablePage").then((m) => ({ default: m.StudentTimetablePage }));
const loadFeesPage = () => import("@/pages/FeesPage").then((m) => ({ default: m.FeesPage }));
const FeesPage = lazy(loadFeesPage);
const StudentTimetablePage = lazy(loadStudentTimetablePage);

type PageLoader = () => Promise<unknown>;

function AppWarmup() {
  const token = useAuthStore((s) => s.token);
  const role = useAuthStore((s) => s.user?.role);

  useEffect(() => {
    if (!token || !role) return;

    const isSuperAdmin = isRole(role, ROLES.SUPER_ADMIN);
    const isTeacher = isRole(role, ROLES.TEACHER);
    const isStudent = isRole(role, ROLES.STUDENT);

    // Warm only pages reachable by this account. Chunks are downloaded in pairs,
    // with a short pause between pairs, so login never triggers a huge burst.
    const loaders: PageLoader[] = isSuperAdmin
      ? [loadCollegesPage, loadCollegeDetailPage]
      : isTeacher
        ? [loadTeacherDashboard, loadClassesPage, loadClassDetailPage, loadTeacherTimetablePage, loadAssignmentsPage, loadSubmissionsPage]
        : isStudent
          ? [loadStudentDashboard, loadStudentClassesPage, loadStudentProfilePage, loadStudentEnrollmentsPage, loadStudentAttendancePage, loadStudentAssignmentsPage, loadStudentTimetablePage]
          : [loadAdminDashboard, loadFeesPage, loadDepartmentsPage, loadCoursesPage, loadSubjectsPage, loadTeachersPage, loadStudentsPage, loadRolesPage, loadUsersPage, loadExamsPage, loadExamSchedulePage, loadMarksEntryPage, loadResultsPage, loadAttendancePage, loadClassHolidaysPage, loadAssignmentsPage, loadSubmissionsPage, loadAnnouncementsPage];

    let cancelled = false;
    let index = 0;

    const runNextBatch = async () => {
      if (cancelled || index >= loaders.length) return;

      const batch = loaders.slice(index, index + 2);
      index += batch.length;
      await Promise.allSettled(batch.map((load) => load()));

      if (!cancelled && index < loaders.length) {
        window.setTimeout(runNextBatch, 350);
      }
    };

    void runNextBatch();

    return () => {
      cancelled = true;
    };
  }, [token, role]);

  return null;
}

function RouteFallback() {
  return <BookLoader label="Loading your page…" />;
}

export default function App() {
  return (
    <BrowserRouter>
      <AppWarmup />
      <NavigationLoader />
      <Suspense fallback={<RouteFallback />}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/change-password" element={<ProtectedRoute><ChangePasswordPage /></ProtectedRoute>} />

          <Route path="/colleges" element={<ProtectedRoute role="SUPER_ADMIN"><CollegesPage /></ProtectedRoute>} />
          <Route path="/colleges/:tenantId" element={<ProtectedRoute role="SUPER_ADMIN"><CollegeDetailPage /></ProtectedRoute>} />

          <Route path="/dashboard" element={<ProtectedRoute staffOnly><DashboardPage /></ProtectedRoute>} />
          <Route path="/admin/dashboard" element={<ProtectedRoute role="ADMIN"><AdminDashboard /></ProtectedRoute>} />
          <Route path="/departments" element={<ProtectedRoute><DepartmentsPage /></ProtectedRoute>} />
          <Route path="/courses" element={<ProtectedRoute><CoursesPage /></ProtectedRoute>} />
          <Route path="/subjects" element={<ProtectedRoute><SubjectsPage /></ProtectedRoute>} />
          <Route path="/teachers" element={<ProtectedRoute><TeachersPage /></ProtectedRoute>} />
          <Route path="/students" element={<ProtectedRoute><StudentsPage /></ProtectedRoute>} />
          <Route path="/roles" element={<ProtectedRoute><RolesPage /></ProtectedRoute>} />
          <Route path="/users" element={<ProtectedRoute><UsersPage /></ProtectedRoute>} />
          <Route path="/exams" element={<ProtectedRoute><ExamsPage /></ProtectedRoute>} />
          <Route path="/exams/:examId/schedule" element={<ProtectedRoute><ExamSchedulePage /></ProtectedRoute>} />
          <Route path="/exam-schedules/:scheduleId/marks" element={<ProtectedRoute><MarksEntryPage /></ProtectedRoute>} />
          <Route path="/results" element={<ProtectedRoute><ResultsPage /></ProtectedRoute>} />
          <Route path="/attendance" element={<ProtectedRoute><AttendancePage /></ProtectedRoute>} />
          <Route path="/attendance/holidays" element={<ProtectedRoute><ClassHolidaysPage /></ProtectedRoute>} />
          <Route path="/assignments" element={<ProtectedRoute><AssignmentsPage /></ProtectedRoute>} />
          <Route path="/submissions" element={<ProtectedRoute><SubmissionsPage /></ProtectedRoute>} />
          <Route path="/announcements" element={<ProtectedRoute><AnnouncementsPage /></ProtectedRoute>} />
          <Route path="/fees" element={<ProtectedRoute><FeesPage /></ProtectedRoute>} />

          <Route path="/teacher/dashboard" element={<ProtectedRoute role="TEACHER"><TeacherDashboard /></ProtectedRoute>} />
          <Route path="/teacher/classes" element={<ProtectedRoute role="TEACHER"><ClassesPage /></ProtectedRoute>} />
          <Route path="/teacher/classes/:id" element={<ProtectedRoute role="TEACHER"><ClassDetailPage /></ProtectedRoute>} />
          <Route path="/teacher/timetable" element={<ProtectedRoute role="TEACHER"><TeacherTimetablePage /></ProtectedRoute>} />

          <Route path="/student/dashboard" element={<ProtectedRoute role="STUDENT"><StudentDashboard /></ProtectedRoute>} />
          <Route path="/student/classes" element={<ProtectedRoute role="STUDENT"><StudentClassesPage /></ProtectedRoute>} />
          <Route path="/student/profile" element={<ProtectedRoute role="STUDENT"><StudentProfilePage /></ProtectedRoute>} />
          <Route path="/student/enrollments" element={<ProtectedRoute role="STUDENT"><StudentEnrollmentsPage /></ProtectedRoute>} />
          <Route path="/student/attendance" element={<ProtectedRoute role="STUDENT"><StudentAttendancePage /></ProtectedRoute>} />
          <Route path="/student/assignments" element={<ProtectedRoute role="STUDENT"><StudentAssignmentsPage /></ProtectedRoute>} />
          <Route path="/student/timetable" element={<ProtectedRoute role="STUDENT"><StudentTimetablePage /></ProtectedRoute>} />
          <Route path="/student/fees" element={<ProtectedRoute role="STUDENT"><FeesPage /></ProtectedRoute>} />

          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
