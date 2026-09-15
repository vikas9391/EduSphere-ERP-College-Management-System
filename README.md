# EduSphere ERP — College Management System

A multi-tenant College ERP/SaaS platform for managing colleges, users, academics, classes, students, teachers, attendance, assignments, examinations, marks, results, timetables, holidays and announcements.

> **Status:** Core ERP architecture is implemented and the latest CI build is green. The project is in the integration/deployment-hardening stage rather than the initial architecture stage.

## Project Structure

```text
EduSphere-ERP-College-Management-System/
├── Backend/        # Spring Boot REST API
├── frontend-web/   # React + TypeScript application
├── docs/            # Project documentation and status
└── .github/         # CI configuration
```

## Technology Stack

### Frontend
- React 19
- TypeScript
- Vite
- React Router
- Axios
- Zustand
- Tailwind CSS v4
- Framer Motion
- Lucide React
- Recharts

### Backend
- Java 21
- Spring Boot 3.5.3
- Spring Security
- Spring Data JPA / Hibernate
- Spring Validation
- Spring Cache / Redis
- Spring Mail
- Flyway
- PostgreSQL
- JJWT
- MapStruct
- Lombok
- SpringDoc OpenAPI
- Maven

## Core Academic Relationship Model

EduSphere now uses one authoritative class-scoped operational model:

```text
Department → Course → Subject

Teacher → SchoolClass
SchoolClass → ClassStudent → Student
SchoolClass → ClassSubject → Subject + Teacher
ClassSubject → ClassEnrollment → Student

ClassEnrollment → Attendance
ClassSubject → TimetableEntry
ClassSubject → Assignment
ClassSubject → ExamSchedule
ClassEnrollment → Marks
```

### Relationship rules

- **ClassStudent** — establishes that a student belongs to a class.
- **ClassSubject** — identifies the subject being taught in a specific class and its assigned teacher.
- **ClassEnrollment** — identifies one student's participation in one ClassSubject.
- **Attendance** — belongs to the exact ClassEnrollment.
- **TimetableEntry** — belongs to a ClassSubject.
- **Assignment** — belongs to a ClassSubject.
- **ExamSchedule** — belongs to a ClassSubject.
- **Marks** — belong to the student's ClassEnrollment and an exam.

The old standalone operational `Enrollment` model has been removed. The repository should not use a subject-only fallback for attendance, assignments, exams, marks or timetable data.

## Student Identity

Student self-service operations resolve the authenticated account through the student's profile:

```text
Authenticated User
       ↓
Student
       ↓
Student.id
       ↓
ClassEnrollment / academic records
```

A `User.id` must never be assumed to be the same identifier as `Student.id`.

## Implemented Features

### Authentication and authorization

- Super-admin authentication.
- Tenant/staff authentication.
- Teacher and student authentication.
- JWT access and refresh tokens.
- Password change flow.
- Forgot-password/reset flow.
- Role/permission-based staff access.
- Frontend role guards for ADMIN, TEACHER and STUDENT routes.
- Backend authorization remains the actual security boundary.

### Multi-tenancy

- Schema-based PostgreSQL multi-tenancy.
- Public tenant metadata separated from tenant-specific academic data.
- Public migrations under `Backend/src/main/resources/db/migration`.
- Tenant migrations under `Backend/src/main/resources/db/tenant-migration`.
- Hibernate schema generation disabled; Flyway owns schema creation.

### Classes and academic structure

- Create/delete school classes.
- Manage class membership through ClassStudent.
- Add/remove students from classes.
- Create ClassSubjects, including bulk creation.
- Assign teachers to ClassSubjects.
- Optional formal Subject/curriculum link.
- View class rosters and class enrollments.
- Student and teacher academic views use ClassEnrollment/ClassSubject relationships.

### Attendance

Attendance is fully class-scoped:

```text
Student → ClassEnrollment → ClassSubject → Teacher + Subject
                              ↓
                           Attendance
```

Implemented:

- Attendance creation/update/view through ClassEnrollment.
- Teacher authorization based on the assigned ClassSubject teacher.
- Student self-only attendance access.
- Admin/super-admin management.
- Unique attendance per `ClassEnrollment + attendanceDate`.
- Attendance summary and subject-wise calculations.
- Canonical statuses:
  - `PRESENT`
  - `ABSENT`
  - `LATE`
  - `EXCUSED`
  - `HOLIDAY`
- `PRESENT` and `LATE` count as attended.
- `ABSENT` counts as missed.
- `EXCUSED` and `HOLIDAY` do not count in the percentage denominator.
- Class holiday calendar API and student holiday display.

### Timetable

Implemented:

- ClassSubject-based timetable entries.
- Student timetable derived from ClassEnrollment → ClassSubject → TimetableEntry.
- Teacher timetable derived from the teacher's ClassSubjects.
- Class conflict detection.
- Teacher conflict detection.
- Create, edit and delete timetable entries.
- Flexible teacher timetable grid with selectable days.
- Add Period/Slot and custom row support.
- Edit/delete controls in the grid.
- Validation when a ClassSubject has no assigned teacher.
- AI-assisted timetable image/PDF inspection and review workflow.

#### AI timetable import

The current flow is:

```text
Image/PDF
   ↓
Backend inspection
   ↓
OpenAI vision/file processing
   ↓
Normalized timetable candidates
   ↓
Teacher/admin review and editing
   ↓
Existing timetable conflict validation
   ↓
Save entries
```

The API key stays on the backend. The frontend never receives `OPENAI_API_KEY`.

### Assignments

- Assignments are tied to ClassSubject.
- Teacher ownership is derived from ClassSubject.teacher.
- Students see assignments for their class-subject enrollments.
- Assignment submission is tied to the student's ClassEnrollment.
- Teacher/admin submission review is class-scoped.
- Legacy subject-only assignment relationships were removed.

### Examinations, marks and results

- Exam schedules use ClassSubject.
- Marks use ClassEnrollment.
- Student eligibility and result calculations use the class-scoped academic relationship.
- Teacher access is constrained by the relevant ClassSubject.
- Legacy standalone enrollment paths were removed.

### Student and teacher portals

- Role-specific dashboards and route protection.
- Student profile, classes, enrollments, attendance, assignments and timetable views.
- Teacher timetable, students, assignments, attendance and academic management views.
- Student class-enrollment responses include class, subject, teacher and academic context.

### Frontend API/authentication layer

- Central Axios API client.
- Environment-driven API base URL through `VITE_API_URL`.
- Access-token injection.
- Refresh-token handling with queued retry behavior.
- API response envelope unwrapping.
- Protected role-based routes.

### Database/migration cleanup

The fresh-database model no longer carries the old standalone enrollment/attendance compatibility path. Obsolete migration-era files and reconciliation/backfill migrations were removed where they belonged to the previous model.

The repository is intentionally maintained around a clean database reset rather than a historical data migration from the old enrollment design.

## API Areas

Current major API areas include:

```text
/api/courses
/api/departments
/api/subjects
/api/teachers
/api/students
/api/classes
/api/marks
/api/exams
/api/timetable
/api/assignments
/api/roles
/api/student
/api/teacher
```

Student class enrollments are exposed under the student portal rather than a generic standalone enrollment API:

```text
/api/student/enrollments
```

Swagger/OpenAPI:

```text
/api-docs
/swagger-ui.html
```

## Environment Variables

### Frontend

```env
VITE_API_URL=http://localhost:8080/api
```

For production, set `VITE_API_URL` to the deployed backend API base URL, including `/api`.

### Backend — required/core

```env
DB_URL=jdbc:postgresql://localhost:5432/college_erp
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=replace_with_a_long_random_secret
FRONTEND_URL=http://localhost:5173
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

### Backend — AI timetable import

```env
OPENAI_API_KEY=
OPENAI_TIMETABLE_MODEL=gpt-5-mini
```

`OPENAI_API_KEY` is optional unless AI timetable inspection is used. It must only be configured on the backend.

### Optional mail configuration

```env
MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
PASSWORD_RESET_TOKEN_EXPIRY_MINUTES=30
```

Never commit real secrets to Git.

## Local Development

### Prerequisites

- Java 21
- Maven or the included Maven wrapper
- Node.js and npm
- PostgreSQL
- Redis

### Start backend

```bash
cd Backend
./mvnw spring-boot:run
```

Windows:

```powershell
cd Backend
./mvnw.cmd spring-boot:run
```

Backend:

```text
http://localhost:8080
```

### Start frontend

```bash
cd frontend-web
npm install
npm run dev
```

Frontend:

```text
http://localhost:5173
```

### Frontend checks

```bash
cd frontend-web
npm run build
npm run lint
```

## CI / Automated Verification

The repository has GitHub Actions CI covering:

- Backend compilation with Java 21.
- Relationship-focused backend tests.
- Frontend installation and production build with Node 22.

Latest verified CI state before this documentation update:

- **Backend:** PASS
- **Relationship tests:** 9 tests, 0 failures, 0 errors
- **Frontend build:** PASS

The relationship test suite currently covers attendance, assignment submission, marks class scope, results and timetable behavior.

## Fresh Database Setup

This project is maintained around a fresh database baseline. When resetting the ERP database, reset Flyway history together with the tenant schemas and recreate the schema from the current migration set.

Do not reintroduce historical backfill, compatibility or reconciliation logic for the removed standalone enrollment model.

## Production / Deployment Status

The application has environment-driven database, JWT, CORS, frontend URL, mail and AI configuration. The frontend API URL is also environment-driven.

However, a hosting provider has **not** been selected in this repository, so provider-specific configuration such as Vercel rewrites, Netlify redirects, Apache/Hostinger `.htaccess`, Docker deployment or Render/Railway service definitions has intentionally not been hard-coded.

See [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) for the provider-neutral deployment checklist.

## What Is Done vs What Is Left

### Done

- [x] Core Spring Boot + React ERP structure.
- [x] Multi-tenant architecture.
- [x] JWT authentication and refresh flow.
- [x] Role/permission enforcement and frontend role guards.
- [x] Class, student and teacher relationships.
- [x] ClassSubject with class + subject + teacher relationship.
- [x] ClassEnrollment as the authoritative student/subject participation record.
- [x] Class-scoped attendance.
- [x] Attendance summaries and holiday handling.
- [x] Class-scoped assignments and submissions.
- [x] Class-scoped exams, marks and results.
- [x] Class-scoped timetable.
- [x] Timetable conflict validation.
- [x] Flexible teacher timetable grid.
- [x] AI timetable image/PDF inspection and review UI.
- [x] Legacy standalone enrollment operational path removed.
- [x] Obsolete attendance/enrollment compatibility paths cleaned up.
- [x] Environment-driven frontend/backend configuration.
- [x] CI build and relationship tests passing.

### Remaining / recommended before production

- [ ] Choose the production hosting target and add only the required provider-specific SPA/deployment configuration.
- [ ] Create production environment variables and verify CORS/frontend API URL against the real domains.
- [ ] Run a full clean-database deployment test, including tenant creation and Flyway migrations.
- [ ] Run manual end-to-end tests for admin → class → ClassSubject → teacher → student → attendance/timetable/assignments/exams/marks/results.
- [ ] Test tenant isolation with at least two tenants.
- [ ] Harden AI timetable import confirmation so a multi-slot import cannot leave a partially saved timetable if a later slot fails validation.
- [ ] Review and resolve the current frontend npm audit findings before production if they are actionable without breaking the application.
- [ ] Optionally update GitHub Actions `actions/checkout` to the current major version to remove the Node 20 deprecation warning.
- [ ] Add production monitoring/logging and a database backup/restore procedure.
- [ ] Perform a final security review of production secrets, CORS, JWT settings, file upload limits and public endpoints.

These items are deployment/integration hardening tasks; they do not mean the core class-scoped ERP architecture is unfinished.

## Documentation

- [`docs/PROJECT_STATUS.md`](docs/PROJECT_STATUS.md) — implementation status, completed areas and remaining work.
- [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) — provider-neutral production deployment checklist.

## Repository

https://github.com/vikas9391/EduSphere-ERP-College-Management-System
