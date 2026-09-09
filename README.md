# EduSphere ERP — College Management System

A multi-tenant College ERP/SaaS platform for managing academic operations, users, classes, students, teachers, attendance, assignments, examinations, marks, results, timetables, announcements and college administration.

> This README documents the implementation currently present in the repository. It is intended to be the main project guide for developers, evaluators and future deployment work.

## Project Overview

EduSphere is structured as a separate web frontend and Spring Boot backend:

```text
EduSphere-ERP-College-Management-System/
├── Backend/                 # Spring Boot REST API
├── frontend-web/            # React + TypeScript web application
├── docs/                    # Architecture and academic relationship documentation
├── .github/                 # GitHub configuration/workflows
└── .gitignore
```

The repository currently has no root README; this document is the new top-level project documentation. The frontend also contains its own README, but that file is still the default Vite template and is not the appropriate project-level documentation.

## Main Capabilities

### Platform / SaaS administration
- Super-admin authentication
- College/tenant management
- Tenant detail management
- Tenant subscription fields and lifecycle data
- Tenant-aware academic data
- Role and permission management
- User management

### Academic administration
- Departments
- Courses
- Subjects
- Teachers
- Students
- Classes
- Class membership
- Subject/teacher assignment to classes
- Student enrollment

### Teaching and student operations
- Teacher dashboard
- Teacher classes
- Teacher timetable
- Student dashboard
- Student profile
- Student classes
- Student timetable
- Student enrollments
- Attendance
- Assignments
- Assignment submissions
- Announcements

### Examination and assessment
- Exams
- Exam schedules
- Marks entry
- Results
- Academic result calculations

The frontend routing and API modules expose these areas directly. The application uses protected routes and role-based routes for Super Admin, Teacher and Student experiences.

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
- JWT decoding
- Oxlint

The frontend is configured as a Vite application on port `5173`. Its API base URL is controlled through `VITE_API_URL` and defaults to `http://localhost:8080/api`.

### Backend

- Java 21
- Spring Boot 3.5.3
- Spring Web
- Spring Security
- Spring Data JPA / Hibernate
- Spring Validation
- Spring Cache
- Spring Data Redis
- Spring Mail
- Spring Actuator
- Flyway
- PostgreSQL
- JJWT
- MapStruct
- Lombok
- SpringDoc OpenAPI / Swagger UI
- Maven

The backend runs on port `8080` and starts from `Backend/src/main/java/com/collegeerp/Backend/BackendApplication.java`.

## Architecture

The backend is organized by domain rather than placing all controllers/services/entities in one package. Current domain areas include:

```text
announcement
assignment
attendance
auth
common
config
course
department
enrollment
examination
marks
result
schoolClass
student
subject
teacher
...
```

The frontend follows a similar feature-oriented structure with API modules, pages, components, stores and configuration.

## Frontend Architecture

The application uses `BrowserRouter`, lazy-loaded pages and a `ProtectedRoute` layer.

Important routes include:

```text
/login
/super-admin/login
/forgot-password
/reset-password
/change-password
/colleges
/dashboard
/admin/dashboard
/departments
/courses
/subjects
/teachers
/students
/roles
/users
/exams
/exams/:examId/schedule
/exam-schedules/:scheduleId/marks
/results
/enrollments
/attendance
/assignments
/submissions
/announcements
/teacher/dashboard
/teacher/classes
/teacher/classes/:id
/teacher/timetable
/student/dashboard
/student/classes
/student/profile
/student/enrollments
/student/attendance
/student/assignments
/student/timetable
```

The frontend API layer exports separate clients for authentication, tenants, departments, courses, teachers, students, subjects, enrollments, attendance, assignments, student profile/portal, exams, exam schedules, marks, results, classes, roles, users and announcements.

Authentication state is maintained with Zustand. Access and refresh tokens are stored in local storage. Axios attaches the access token to requests and contains a refresh flow for expired access tokens so concurrent `401` responses can share a single refresh request.

## Backend API

The REST API uses the `/api` prefix. Representative controllers include:

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

The backend uses Spring Security and method-level authorization such as `@PreAuthorize` on protected controllers. The frontend permission model is intended for UI visibility; backend authorization remains the enforcement boundary.

Swagger/OpenAPI is configured at:

```text
/api-docs
/swagger-ui.html
```

## Authentication and Authorization

The system supports:

- Super Admin authentication
- Tenant/staff authentication
- Teacher access
- Student access
- JWT access tokens
- JWT refresh tokens
- Password change
- Forgot-password flow
- Password reset tokens
- Role/permission-based staff access

Current token configuration in the backend is:

```text
Access token: 15 minutes
Refresh token: 7 days
Password reset token: 30 minutes by default
```

Never commit `JWT_SECRET`, database credentials, mail credentials or Redis credentials to Git.

## Multi-Tenancy

The backend is designed as a schema-based multi-tenant SaaS system. The public database contains tenant metadata, including:

- Tenant ID
- Tenant name
- Schema name
- Subdomain
- Active status
- Subscription plan
- Subscription status
- Subscription expiration

The backend enables Hibernate schema-based multi-tenancy and uses Flyway for database migrations. Tenant-specific migrations are maintained separately from public-schema migrations.

Conceptually:

```text
                    Public PostgreSQL schema
                              │
                ┌─────────────┴─────────────┐
                │                           │
             tenants                  super_admins
                │
       ┌────────┼────────┐
       │        │        │
   tenant_a  tenant_b  tenant_c
    schema    schema    schema
       │        │        │
       └── academic data ──┘
```

## Academic Data Model

The repository already contains a dedicated academic relationship master plan. The intended authoritative model is:

```text
Department → Course → Subject

Teacher → SchoolClass
SchoolClass → ClassStudent → Student
SchoolClass → ClassSubject → Subject + Teacher
ClassSubject → ClassEnrollment → Student

ClassEnrollment → Attendance
ClassSubject → Assignment
ClassSubject → ExamSchedule → Marks/Results
```

This model is important because the repository currently contains legacy/overlapping enrollment and attendance relationships. The documented migration plan recommends making `ClassEnrollment` the authoritative operational student-subject relationship.

### Important identity rule

For student self-service operations, do not assume authenticated `User.id` is the same as `Student.id`.

The intended resolution is:

```text
Authenticated User
       ↓
Student profile
       ↓
Student.id
       ↓
Academic records
```

This prevents cross-entity ID collisions and keeps student-specific operations correctly scoped.

## Attendance Model

The current migration path supports both legacy enrollment-based attendance and the newer class-enrollment relationship.

The tenant migration adds:

```text
attendance.class_enrollment_id
```

and a uniqueness rule for class-enrollment/date combinations.

The long-term target is:

```text
ClassEnrollment → Attendance
```

The existing academic relationship plan explicitly recommends keeping compatibility fields during migration and removing them only after existing data has been reconciled and verified.

## Database and Migrations

PostgreSQL is the runtime database. Hibernate schema auto-generation is disabled:

```text
spring.jpa.hibernate.ddl-auto=none
```

Flyway is enabled for database migrations.

The repository contains public migrations for areas such as:

- Tenant creation
- Super-admin creation
- Tenant subscription information
- Super-admin password reset tokens
- Later attendance integrity/versioning

Tenant-specific schema migrations are kept under the tenant migration path.

## Redis

Redis is used through Spring's cache abstraction. The repository includes a dedicated Redis setup guide.

For local development:

```bash
docker run --name college-erp-redis -p 6379:6379 -d redis:7-alpine
```

Default Redis settings documented by the project:

```text
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_CACHE_TTL_SECONDS=600
REDIS_KEY_PREFIX=college-erp:
```

Tenant-specific cache keys must include tenant identity whenever the cached data is tenant-specific.

## Environment Variables

### Frontend

Create `frontend-web/.env` from `.env.example`:

```env
VITE_API_URL=http://localhost:8080/api
```

### Backend

The backend expects at least:

```env
DB_URL=jdbc:postgresql://localhost:5432/college_erp
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=replace_with_a_long_random_secret
FRONTEND_URL=http://localhost:5173
```

Mail configuration is optional for local development but should be configured in production:

```env
MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
PASSWORD_RESET_TOKEN_EXPIRY_MINUTES=30
```

Redis configuration should be provided when running the cache infrastructure outside its local defaults.

## Local Development

### Prerequisites

Install:

- Java 21
- Maven or use the included Maven wrapper
- Node.js compatible with the frontend toolchain
- npm
- PostgreSQL
- Redis

### 1. Start PostgreSQL

Create a database, for example:

```sql
CREATE DATABASE college_erp;
```

Set the backend database environment variables before starting Spring Boot.

### 2. Start Redis

```bash
docker run --name college-erp-redis -p 6379:6379 -d redis:7-alpine
```

If Redis is already installed locally, simply run it on port `6379`.

### 3. Start the backend

Windows:

```powershell
cd Backend
./mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
cd Backend
./mvnw spring-boot:run
```

The backend is configured for:

```text
http://localhost:8080
```

### 4. Start the frontend

```bash
cd frontend-web
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

### Production frontend build

```bash
cd frontend-web
npm run build
npm run preview
```

### Frontend lint

```bash
cd frontend-web
npm run lint
```

## Security Notes

The repository already includes several good security foundations:

- Spring Security
- JWT authentication
- Refresh-token flow
- Method-level authorization
- Password reset tokens
- Environment-based secrets
- Minimal Actuator exposure
- Backend validation
- Tenant-aware data architecture

Before production deployment, verify all tenant boundaries with integration tests. In particular, student, teacher, enrollment, attendance, assignment, exam and marks operations must validate the authenticated user's relationship to the academic record being accessed.

Also verify that production deployments use strong JWT secrets, HTTPS, secure cookie/token policies where applicable, restricted CORS, rate limiting on authentication endpoints, secure database credentials and protected Swagger/Actuator exposure as appropriate.

## Known Architecture Work

The repository contains an explicit academic relationship migration plan. The main architectural item to finish is consolidating overlapping student-subject paths around:

```text
ClassStudent
ClassSubject
ClassEnrollment
```

and then making the same relationship model authoritative for:

- Attendance
- Assignments
- Exams
- Marks
- Results
- Student enrollment views
- Teacher views

The documented plan also calls for reconciling old data before removing compatibility fields. This is a data-integrity migration, not merely a frontend change.

## Recommended Verification Checklist

Before calling the ERP production-ready, verify:

- [ ] Backend starts against a clean PostgreSQL database.
- [ ] Flyway migrations complete successfully.
- [ ] Tenant creation creates the correct tenant metadata/schema.
- [ ] Super-admin authentication works.
- [ ] Tenant user authentication works.
- [ ] Access-token refresh works after expiry.
- [ ] Password reset works with configured SMTP.
- [ ] Role and permission checks are enforced server-side.
- [ ] Students cannot access another student's records.
- [ ] Teachers cannot edit classes/subjects outside their authorization.
- [ ] Class membership is authoritative.
- [ ] ClassSubject identifies the subject and teacher for a class.
- [ ] ClassEnrollment is authoritative for student participation.
- [ ] Attendance is consistent with ClassEnrollment.
- [ ] Assignments are scoped to the correct class/subject.
- [ ] Exams, marks and results use the same academic relationships.
- [ ] Announcements resolve recipients using the correct Student identity.
- [ ] Tenant data cannot leak across schemas.
- [ ] Redis cache keys include tenant identity for tenant-specific data.
- [ ] Frontend build succeeds.
- [ ] Frontend lint succeeds.
- [ ] API error messages are handled consistently.
- [ ] Production secrets are not committed.

## Documentation

Additional architecture documentation is available in:

```text
docs/ACADEMIC_RELATIONSHIP_MASTER_PLAN.md
Backend/REDIS_SETUP.md
```

The academic master plan contains the detailed migration strategy and acceptance criteria for the Student/Teacher/Class/Subject/Enrollment/Attendance model.

## Project Status

**Current stage:** feature-rich ERP application under active development and architectural hardening.

The repository has a substantial working foundation across administration, academics, student/teacher portals, assessments, attendance, authentication and multi-tenancy. The next major engineering priority is completing and validating the academic relationship consolidation and production deployment hardening rather than adding unrelated modules.

## License

No explicit license file was identified at the repository root during this audit. Add a `LICENSE` file before distributing the project as open source or commercially licensing it.

## Repository

[EduSphere ERP — College Management System](https://github.com/vikas9391/EduSphere-ERP-College-Management-System)
