# EduSphere ERP — College Management System

A multi-tenant College ERP/SaaS platform for managing colleges, users, academics, classes, students, teachers, attendance, assignments, examinations, marks, results, timetables, holidays and announcements.

> **Status:** Core ERP architecture is implemented. The web application uses React + TypeScript, and the mobile application in `app/` is now a Flutter/Dart application. The former Expo/React Native mobile implementation has been removed.

## Project Structure

```text
EduSphere-ERP-College-Management-System/
├── Backend/        # Spring Boot REST API
├── frontend-web/   # React + TypeScript web application
├── app/            # Flutter + Dart mobile application
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
- Flutter stable
- Dart 3.5+
- Material 3
- `http`
- `shared_preferences`
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

## Flutter Mobile App

The `app/` directory is the single mobile client for EduSphere. It is implemented in **Flutter/Dart** and connects directly to the same Spring Boot API used by the web application.

The previous Expo/React Native project has been removed from the mobile app directory. There are no React Native/Expo mobile dependencies, TypeScript mobile entry points, or Expo configuration files in the current mobile project.

### Current Flutter mobile foundation

- Flutter/Dart project structure.
- Material 3 green/white EduSphere branding.
- College code + username + password login.
- JWT access/refresh token handling.
- Persistent session storage with `shared_preferences`.
- Student, teacher and admin role-aware home/dashboard foundation.
- Logout flow.
- Backend REST integration through the shared API.
- Flutter static analysis in CI.

### Run the mobile app

```bash
cd app
flutter pub get
flutter run --dart-define=API_URL=http://10.0.2.2:8080/api
```

For a physical device, use a backend URL reachable from that device.

### Build Android APK

```bash
cd app
flutter build apk --release --dart-define=API_URL=https://your-backend.example.com/api
```

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

### Flutter Mobile

The mobile API URL is supplied at build/run time with Dart's `--dart-define`:

```bash
flutter run --dart-define=API_URL=http://10.0.2.2:8080/api
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

### Flutter Mobile

```bash
cd app
flutter pub get
flutter run --dart-define=API_URL=http://10.0.2.2:8080/api
```

## CI / Verification

CI validates all three application layers:

- Backend compilation and relationship isolation tests.
- Frontend production build.
- Flutter mobile dependency resolution and `flutter analyze`.

The mobile CI no longer installs Node/Expo dependencies or runs a TypeScript type check.

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
- [x] Web CI/build and relationship tests.
- [x] Flutter mobile project replacing the former React Native app.
- [x] Flutter mobile login and backend connection foundation.
- [x] Flutter mobile session persistence and refresh-token foundation.
- [x] Flutter mobile static analysis in CI.

### Remaining

- [ ] Complete all Student Flutter modules: classes, attendance, assignments, timetable, exams, marks and results.
- [ ] Complete all Teacher Flutter modules: classes/rosters, attendance, assignments, timetable, exams and marks.
- [ ] Add appropriate Admin/Super-admin Flutter views.
- [ ] Add Flutter profile/password/reset flows.
- [ ] Add robust network/offline states.
- [ ] Add mobile push notifications if required.
- [ ] Add Android/iOS production icons, splash assets and release metadata.
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
