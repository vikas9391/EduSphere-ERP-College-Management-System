# EduSphere ERP — College Management System

A multi-tenant College ERP/SaaS platform for managing colleges, users, academics, classes, students, teachers, attendance, assignments, examinations, marks, results, timetables and announcements.

## Project Structure

```text
EduSphere-ERP-College-Management-System/
├── Backend/        # Spring Boot REST API
├── frontend-web/   # React + TypeScript application
├── docs/            # Project documentation
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

## Academic Relationship Model

EduSphere uses one authoritative operational relationship model:

```text
Department → Course → Subject

Teacher → SchoolClass
SchoolClass → ClassStudent → Student
SchoolClass → ClassSubject → Subject + Teacher
ClassSubject → ClassEnrollment → Student

ClassEnrollment → Attendance
ClassSubject → Assignment
ClassSubject → ExamSchedule
ClassEnrollment → Marks
```

### Meaning of each relationship

- **ClassStudent** — the student belongs to the class.
- **ClassSubject** — a subject is taught in a specific class by a specific teacher.
- **ClassEnrollment** — the exact student's participation in that taught subject.
- **Attendance** — attendance for that exact ClassEnrollment.
- **TimetableEntry** — schedule for a ClassSubject.
- **Assignment** — assignment for a ClassSubject.
- **ExamSchedule** — exam schedule for a ClassSubject.
- **Marks** — marks for the student's ClassEnrollment in an exam.

The operational academic data must not fall back to a separate subject-only student relationship.

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

## Attendance

Attendance is strictly class-scoped:

```text
Student → ClassEnrollment → ClassSubject → Teacher + Subject
                              ↓
                           Attendance
```

A teacher can manage attendance only for ClassSubjects assigned to that teacher. A student can view only their own attendance.

The attendance statuses are:

```text
PRESENT
ABSENT
LATE
EXCUSED
```

`PRESENT` and `LATE` count as attended. `ABSENT` counts as missed. `EXCUSED` is excluded from the attendance percentage denominator.

Attendance is unique per `ClassEnrollment + attendanceDate`.

## Timetable

Student timetable entries are derived from the student's ClassEnrollments and their ClassSubjects:

```text
Student
  ↓
ClassEnrollment
  ↓
ClassSubject
  ↓
TimetableEntry
```

Teacher timetable entries are derived from ClassSubjects assigned to that teacher. Timetable conflict checks protect both class and teacher schedules.

## Authentication and Authorization

The application supports:

- Super-admin authentication
- Tenant/staff authentication
- Teacher access
- Student access
- JWT access and refresh tokens
- Password change
- Forgot-password/reset flow
- Role/permission-based staff access

Backend authorization is the enforcement boundary; frontend route protection is only the UI layer.

## Multi-Tenancy

The backend uses schema-based PostgreSQL multi-tenancy. Public-schema tenant metadata is separated from tenant-specific academic data.

Tenant migrations are stored under:

```text
Backend/src/main/resources/db/tenant-migration
```

Public migrations are stored under:

```text
Backend/src/main/resources/db/migration
```

The database is intentionally configured with Hibernate schema generation disabled:

```text
spring.jpa.hibernate.ddl-auto=none
```

Flyway is responsible for creating the database schema.

## Environment Variables

### Frontend

```env
VITE_API_URL=http://localhost:8080/api
```

### Backend

```env
DB_URL=jdbc:postgresql://localhost:5432/college_erp
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=replace_with_a_long_random_secret
FRONTEND_URL=http://localhost:5173
```

Optional mail configuration:

```env
MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
PASSWORD_RESET_TOKEN_EXPIRY_MINUTES=30
```

Never commit secrets to Git.

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

## Fresh Database Setup

This repository is maintained around a fresh database baseline. When resetting the ERP database, reset the Flyway history together with the tenant schemas and recreate the schema from the current migration set.

No migration should contain data backfill, historical reconciliation or compatibility handling for the previous enrollment/attendance model.

## Main API Areas

```text
/api/courses
/api/departments
/api/subjects
/api/teachers
/api/students
/api/enrollments
/api/marks
/api/exams
/api/timetable
/api/assignments
/api/roles
/api/student
```

Swagger/OpenAPI:

```text
/api-docs
/swagger-ui.html
```

## Production Verification

Before production deployment, verify:

- [ ] Backend starts against a clean PostgreSQL database.
- [ ] Public and tenant Flyway migrations complete successfully.
- [ ] Tenant creation creates the correct schema.
- [ ] Authentication and refresh-token flow work.
- [ ] Student identity resolves User → Student correctly.
- [ ] Class membership uses ClassStudent.
- [ ] ClassSubject correctly identifies class, subject and teacher.
- [ ] ClassEnrollment correctly identifies student participation.
- [ ] Attendance uses only ClassEnrollment.
- [ ] Timetable uses ClassSubject.
- [ ] Assignments use ClassSubject.
- [ ] Exams use ClassSubject.
- [ ] Marks use ClassEnrollment.
- [ ] Students cannot access another student's records.
- [ ] Teachers can manage only their assigned ClassSubjects.
- [ ] Tenant data cannot leak across schemas.
- [ ] Frontend build succeeds.
- [ ] Backend tests pass.
- [ ] Production secrets are not committed.

## Project Status

**Current stage:** active ERP development with a clean class-scoped academic data model.

The project is intentionally starting fresh at the database level. Previous migration-era compatibility and data-reconciliation paths are not part of the new operational model.

## Repository

https://github.com/vikas9391/EduSphere-ERP-College-Management-System
