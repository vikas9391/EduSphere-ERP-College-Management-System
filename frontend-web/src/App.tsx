// src/App.tsx

import { Suspense, lazy } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import { ProtectedRoute } from "@/components/ProtectedRoute";
import { Loader2 } from "lucide-react";

// Auth - loaded eagerly since it's the first thing almost every visitor sees.
import { LoginPage } from "@/pages/LoginPage";
import { SuperAdminLoginPage } from "@/pages/SuperAdminLoginPage";

/**
 * Every other page is route-split via React.lazy(). Previously the whole app
 * (every admin/teacher/student page, every table/form/modal) was bundled into
 * one ~877KB JS chunk loaded on first paint - including pages a given user's
 * role would never even navigate to. Splitting per-route means the login page
 * loads almost instantly, and each subsequent page's code is fetched only when
 * its route is actually visited.
 */

// Super Admin
const CollegesPage = lazy(() => import("@/pages/Collegespage").then((m) => ({ default: m.CollegesPage })));
const CollegeDetailPage = lazy(() => import("@/pages/CollegeDetailPage").then((m) => ({ default: m.CollegeDetailPage })));

// Admin
const DashboardPage = lazy(() => import("@/pages/DashboardPage").then((m) => ({ default: m.DashboardPage })));
const AdminDashboard = lazy(() => import("@/pages/DashboardPage").then((m) => ({ default: m.AdminDashboard })));
const DepartmentsPage = lazy(() => import("@/pages/DepartmentsPage").then((m) => ({ default: m.DepartmentsPage })));
const CoursesPage = lazy(() => import("@/pages/CoursesPage").then((m) => ({ default: m.CoursesPage })));
const SubjectsPage = lazy(() => import("@/pages/SubjectsPage").then((m) => ({ default: m.SubjectsPage })));
const TeachersPage = lazy(() => import("@/pages/TeachersPage").then((m) => ({ default: m.TeachersPage })));
const StudentsPage = lazy(() => import("@/pages/student/StudentsPage").then((m) => ({ default: m.StudentsPage })));

// Examination, Marks, Result
const ExamsPage = lazy(() => import("@/pages/ExamsPage").then((m) => ({ default: m.ExamsPage })));
const ExamSchedulePage = lazy(() => import("@/pages/ExamSchedulePage").then((m) => ({ default: m.ExamSchedulePage })));
const MarksEntryPage = lazy(() => import("@/pages/MarksEntryPage").then((m) => ({ default: m.MarksEntryPage })));
const ResultsPage = lazy(() => import("@/pages/ResultsPage").then((m) => ({ default: m.ResultsPage })));

// Enrollment
const EnrollmentsPage = lazy(() => import("@/pages/EnrollmentsPage").then((m) => ({ default: m.EnrollmentsPage })));

// Attendance
const AttendancePage = lazy(() => import("@/pages/AttendancePage").then((m) => ({ default: m.AttendancePage })));

// Assignment
const AssignmentsPage = lazy(() => import("@/pages/AssignmentsPage").then((m) => ({ default: m.AssignmentsPage })));

// Submission
const SubmissionsPage = lazy(() => import("@/pages/Submissionspage").then((m) => ({ default: m.SubmissionsPage })));

// Teacher
const TeacherDashboard = lazy(() => import("@/pages/Teacherdashboard ").then((m) => ({ default: m.TeacherDashboard })));
const ClassesPage = lazy(() => import("@/pages/ClassesPage").then((m) => ({ default: m.ClassesPage })));
const ClassDetailPage = lazy(() => import("@/pages/ClassDetailPage").then((m) => ({ default: m.ClassDetailPage })));

// Student
const StudentDashboard = lazy(() => import("@/pages/StudentDashboard").then((m) => ({ default: m.StudentDashboard })));
const StudentProfilePage = lazy(() => import("@/pages/student/StudentProfilePage").then((m) => ({ default: m.StudentProfilePage })));
const StudentEnrollmentsPage = lazy(() => import("@/pages/student/Studentenrollmentspage").then((m) => ({ default: m.StudentEnrollmentsPage })));
const StudentAssignmentsPage = lazy(() => import("@/pages/student/StudentAssignmentsPage").then((m) => ({ default: m.StudentAssignmentsPage })));
const StudentAttendancePage = lazy(() => import("@/pages/StudentAttendancePage.tsx").then((m) => ({ default: m.StudentAttendancePage })));
const StudentClassesPage = lazy(() => import("@/pages/student/StudentClassesPage").then((m) => ({ default: m.StudentClassesPage })));

function RouteFallback() {
  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-parchment">
      <Loader2 className="animate-spin text-brass" size={28} />
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<RouteFallback />}>
        <Routes>

          {/* Public */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/super-admin/login" element={<SuperAdminLoginPage />} />

          {/* Super Admin — college onboarding. The backend now requires a
              SUPER_ADMIN-authenticated caller for /api/tenants/**, so this route
              is gated the same way on the frontend. */}
          <Route
            path="/colleges"
            element={
              <ProtectedRoute role="SUPER_ADMIN">
                <CollegesPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/colleges/:tenantId"
            element={
              <ProtectedRoute role="SUPER_ADMIN">
                <CollegeDetailPage />
              </ProtectedRoute>
            }
          />

          {/* Admin */}
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin/dashboard"
            element={
              <ProtectedRoute>
                <AdminDashboard />
              </ProtectedRoute>
            }
          />

          <Route
            path="/departments"
            element={
              <ProtectedRoute>
                <DepartmentsPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/courses"
            element={
              <ProtectedRoute>
                <CoursesPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/subjects"
            element={
              <ProtectedRoute>
                <SubjectsPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/teachers"
            element={
              <ProtectedRoute>
                <TeachersPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/students"
            element={
              <ProtectedRoute>
                <StudentsPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/exams"
            element={
              <ProtectedRoute>
                <ExamsPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/exams/:examId/schedule"
            element={
              <ProtectedRoute>
                <ExamSchedulePage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/exam-schedules/:scheduleId/marks"
            element={
              <ProtectedRoute>
                <MarksEntryPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/results"
            element={
              <ProtectedRoute>
                <ResultsPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/enrollments"
            element={
              <ProtectedRoute>
                <EnrollmentsPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/attendance"
            element={
              <ProtectedRoute>
                <AttendancePage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/assignments"
            element={
              <ProtectedRoute>
                <AssignmentsPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/submissions"
            element={
              <ProtectedRoute>
                <SubmissionsPage />
              </ProtectedRoute>
            }
          />

          {/* Teacher */}

          <Route
            path="/teacher/dashboard"
            element={
              <ProtectedRoute>
                <TeacherDashboard />
              </ProtectedRoute>
            }
          />

          <Route
            path="/teacher/classes"
            element={
              <ProtectedRoute role="TEACHER">
                <ClassesPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/teacher/classes/:id"
            element={
              <ProtectedRoute role="TEACHER">
                <ClassDetailPage />
              </ProtectedRoute>
            }
          />

          {/* Student */}

          <Route
            path="/student/dashboard"
            element={
              <ProtectedRoute>
                <StudentDashboard />
              </ProtectedRoute>
            }
          />

          <Route
            path="/student/classes"
            element={
              <ProtectedRoute role="STUDENT">
                <StudentClassesPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/student/profile"
            element={
              <ProtectedRoute>
                <StudentProfilePage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/student/enrollments"
            element={
              <ProtectedRoute>
                <StudentEnrollmentsPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/student/attendance"
            element={
              <ProtectedRoute>
                <StudentAttendancePage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/student/assignments"
            element={
              <ProtectedRoute>
                <StudentAssignmentsPage />
              </ProtectedRoute>
            }
          />

          {/* Default */}

          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="*" element={<Navigate to="/login" replace />} />

        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}