# EduSphere ERP — College Management System

A multi-tenant College ERP/SaaS platform for managing colleges, users, academics, classes, students, teachers, attendance, assignments, examinations, marks, results, timetables, holidays and announcements.

> **Status:** Core ERP architecture is implemented and the latest verified web CI is green. A new Expo/React Native mobile app has also been scaffolded in `app/` and is being extended to use the same backend and design system.

## Project Structure

```text
EduSphere-ERP-College-Management-System/
├── Backend/        # Spring Boot REST API
├── frontend-web/   # React + TypeScript web application
├── app/            # Expo + React Native mobile application
├── docs/            # Project documentation, status and deployment guide
└── .github/         # CI configuration
```

## Technology Stack

### Web
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

### Mobile
- Expo
- React Native
- React Navigation
- AsyncStorage
- Same Spring Boot API as the web application

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

EduSphere uses one authoritative class-scoped operational model:

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

The old standalone operational `Enrollment` model has been removed. Attendance, assignments, exams, marks and timetable data use the class-scoped relationships.

## Implemented Features

### Authentication and authorization

- Super-admin, tenant/staff, teacher and student authentication.
- JWT access and refresh tokens.
- Password change and forgot-password/reset flows.
- Role/permission-based staff access.
- Frontend role guards for ADMIN, TEACHER and STUDENT routes.
- Backend authorization remains the actual security boundary.

### Multi-tenancy

- Schema-based PostgreSQL multi-tenancy.
- Public tenant metadata separated from tenant-specific academic data.
- Flyway-managed public and tenant migrations.
- Hibernate schema generation disabled; Flyway owns schema creation.

### Classes and academics

- Create/delete school classes.
- Add/remove students through ClassStudent.
- Create ClassSubjects, including bulk creation.
- Assign teachers to ClassSubjects.
- Optional formal Subject/curriculum link.
- Class rosters and class-enrollment views.

### Attendance and holidays

- Attendance is tied to ClassEnrollment.
- Teacher authorization follows the assigned ClassSubject teacher.
- Student self-only attendance access.
- Admin/super-admin management.
- Unique attendance per ClassEnrollment + date.
- Attendance summaries and subject-wise calculations.
- Statuses: `PRESENT`, `ABSENT`, `LATE`, `EXCUSED`, `HOLIDAY`.
- `PRESENT`/`LATE` count as attended; `ABSENT` counts as missed; `EXCUSED`/`HOLIDAY` are excluded from the percentage denominator.
- Class holiday calendar API and student holiday display.

### Timetable

- ClassSubject-based timetable entries.
- Student and teacher timetable views.
- Class and teacher conflict detection.
- Create/edit/delete timetable entries.
- Flexible teacher timetable grid with selectable days and custom rows.
- Validation when a ClassSubject has no assigned teacher.
- AI-assisted image/PDF timetable inspection and review.

AI timetable flow:

```text
Image/PDF → Backend → OpenAI processing → Candidates → Review/Edit → Conflict validation → Save
```

The OpenAI API key remains backend-only.

### Assignments, examinations, marks and results

- Assignments and submissions are class-scoped.
- Exams use ClassSubject.
- Marks use ClassEnrollment.
- Student eligibility and result calculations use the class-scoped academic model.
- Teacher access is constrained by ClassSubject ownership.

### Web UI

- Role-specific dashboards and route protection.
- Student profile, classes, enrollments, attendance, holidays, assignments and timetable pages.
- Teacher academic management pages.
- Class roster/ClassSubject management.
- Flexible timetable editor and AI import review UI.
- Central Axios API client with `VITE_API_URL`.
- Access-token injection and refresh-token handling.
- Responsive botanical design system.

## Mobile App

The `app/` folder is now the mobile application for EduSphere.

The mobile app is **not a second backend**. It connects to the same Spring Boot API and uses the same tenant/authentication model.

### Current mobile milestone

Implemented:

- Expo/React Native project foundation.
- EduSphere mobile branding.
- Same botanical color system as the web UI.
- College code + username + password login.
- Connection to `/api/auth/login`.
- Session persistence foundation.
- Post-login dashboard shell.
- Sign-out flow.
- Mobile API environment configuration.

### Shared design system

The mobile app uses the web application's current botanical palette:

```text
Primary       #2e7d32
Primary Dark  #256428
Secondary     #4caf50
Light Green   #e8f5e9
Background    #f8f8f2
Card          #ffffff
Text          #1f2937
Muted         #6b7280
Border        #eef2e7
Danger        #c1543c
```

### Run the mobile app

```bash
cd app
npm install
npm start
```

Configure the backend:

```env
EXPO_PUBLIC_API_URL=http://localhost:8080/api
```

For a physical device, use an API URL reachable from the device rather than `localhost`.

## API Areas

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

Student class enrollments are exposed through:

```text
/api/student/enrollments
```

Swagger/OpenAPI:

```text
/api-docs
/swagger-ui.html
```

## Environment Variables

### Web

```env
VITE_API_URL=http://localhost:8080/api
```

### Mobile

```env
EXPO_PUBLIC_API_URL=http://localhost:8080/api
```

### Backend

```env
DB_URL=jdbc:postgresql://localhost:5432/college_erp
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=replace_with_a_long_random_secret
FRONTEND_URL=http://localhost:5173
CORS_ALLOWED_ORIGINS=http://localhost:5173
OPENAI_API_KEY=
OPENAI_TIMETABLE_MODEL=gpt-5-mini
```

Optional mail configuration is documented in `docs/DEPLOYMENT.md`.

Never commit real secrets to Git.

## Local Development

### Backend

```bash
cd Backend
./mvnw spring-boot:run
```

Windows:

```powershell
cd Backend
./mvnw.cmd spring-boot:run
```

Backend: `http://localhost:8080`

### Web

```bash
cd frontend-web
npm install
npm run dev
```

Web: `http://localhost:5173`

### Mobile

```bash
cd app
npm install
npm start
```

## CI / Verification

The latest verified web CI run before adding the mobile scaffold reported:

- Backend compilation: **PASS**
- Relationship tests: **9 tests, 0 failures, 0 errors**
- Frontend production build: **PASS**

The mobile application was added after that verification, so mobile CI/type-check/build automation is still a remaining task.

## Fresh Database Setup

The project is maintained around a fresh database baseline. When resetting the ERP database, reset Flyway history together with tenant schemas and recreate the schema from the current migration set.

Do not reintroduce historical backfill, compatibility or reconciliation logic for the removed standalone enrollment model.

## Production Status

The application has environment-driven database, JWT, CORS, frontend URL, mail and AI configuration. Provider-specific hosting configuration is intentionally not hard-coded until a production host is selected.

See `docs/DEPLOYMENT.md` for the provider-neutral deployment checklist.

## What Is Done vs What Is Left

### Done

- [x] Core Spring Boot + React ERP.
- [x] Multi-tenant architecture.
- [x] JWT authentication/refresh flow.
- [x] Role and permission enforcement.
- [x] Frontend role guards.
- [x] Class/student/teacher relationships.
- [x] ClassSubject and ClassEnrollment model.
- [x] Class-scoped attendance.
- [x] Attendance summaries and holidays.
- [x] Class-scoped assignments/submissions.
- [x] Class-scoped exams, marks and results.
- [x] Class-scoped timetable and conflict validation.
- [x] Flexible timetable grid.
- [x] AI timetable image/PDF inspection and review UI.
- [x] Legacy standalone enrollment path removed.
- [x] Environment-driven API configuration.
- [x] Web CI/build and relationship tests passing.
- [x] Mobile app folder created.
- [x] Mobile app login connected to the same backend.
- [x] Mobile app uses the same botanical UI color system.

### Remaining

- [ ] Complete Student mobile dashboard and modules.
- [ ] Complete Teacher mobile dashboard and modules.
- [ ] Add appropriate Admin/Super-admin mobile views.
- [ ] Add mobile attendance, assignments, timetable, exams, marks and results screens.
- [ ] Add mobile profile/password/reset flows.
- [ ] Add robust mobile refresh-token handling and network/offline states.
- [ ] Add mobile push notifications if required.
- [ ] Add mobile CI/type checking/release builds.
- [ ] Configure Android/iOS production icons, splash assets and release metadata.
- [ ] Test Android/iOS builds on physical devices.
- [ ] Make AI timetable multi-slot confirmation transactional to prevent partial saves.
- [ ] Choose production hosting and add provider-specific SPA configuration.
- [ ] Run clean production database/Flyway deployment testing.
- [ ] Test tenant isolation with multiple tenants.
- [ ] Review npm audit findings safely.
- [ ] Add production monitoring, logging and database backups.
- [ ] Perform final production security review.

## Documentation

- `docs/PROJECT_STATUS.md` — complete implementation status and mobile roadmap.
- `docs/DEPLOYMENT.md` — web/mobile deployment and production checklist.

## Repository

https://github.com/vikas9391/EduSphere-ERP-College-Management-System
