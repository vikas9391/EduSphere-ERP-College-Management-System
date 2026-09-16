# EduSphere ERP — Project Status

## Current status

The core ERP architecture is implemented around a class-scoped academic model. The repository contains:

- `Backend/` — Spring Boot 3.5.3 / Java 21 REST API.
- `frontend-web/` — React 19 + TypeScript + Vite web application.
- `app/` — Expo/React Native mobile application using the same backend and the web application's botanical design system.

## Completed

### Backend
- Multi-tenant PostgreSQL schema architecture.
- JWT authentication and refresh tokens.
- Role and permission enforcement.
- Student, teacher, class and subject management.
- `ClassStudent` for class membership.
- `ClassSubject` for class + subject + teacher assignment.
- `ClassEnrollment` as the authoritative student participation record.
- Class-scoped attendance and attendance summaries.
- `EXCUSED` and `HOLIDAY` attendance statuses excluded from the percentage denominator.
- Class holiday calendar API.
- Class-scoped assignments and submissions.
- Class-scoped exams, marks and results.
- Class-scoped timetable and class/teacher conflict validation.
- Validation for timetable entries whose ClassSubject has no teacher.
- AI timetable image/PDF inspection endpoint with backend-only OpenAI key handling.
- Legacy standalone operational enrollment model removed.
- Obsolete enrollment/attendance compatibility paths removed from the fresh-database architecture.

### Web UI
- Role-specific dashboards and route protection.
- Student profile, classes, enrollments, attendance, holidays, assignments, results and timetable views.
- Teacher academic management views.
- Class roster and ClassSubject management.
- Flexible teacher timetable grid with selectable days, custom rows, add/edit/delete controls.
- AI timetable upload/review UI for image/PDF imports.
- Central API client with environment-driven `VITE_API_URL`.
- Access-token and refresh-token handling.
- Shared botanical design system using green/white/soft-neutral surfaces, rounded cards and responsive layouts.

### Mobile app
The `app/` directory is an active Expo/React Native client backed by the same Spring Boot API.

Implemented:
- Expo + React Native project structure and Android/iOS package identifiers.
- College code, username and password login.
- Persistent access/refresh-token session storage.
- Automatic access-token refresh on protected API `401` responses, with session cleanup when refresh fails.
- Role-aware initial navigation for student, teacher, admin and super-admin accounts.
- Student dashboard with attendance, subject and assignment summary.
- Student classes/enrollments, attendance, assignments, timetable and profile screens.
- Student results screen with semester results, subject marks/grades, SGPA and CGPA.
- Teacher and admin dashboard screens using the same backend authorization model.
- Pull-to-refresh and loading/error states on the main student data screens.
- Environment-driven mobile API URL via `EXPO_PUBLIC_API_URL`.
- Mobile `typecheck` npm script.
- CI job that installs the mobile dependencies and runs the TypeScript type check.

## CI verification

The CI workflow now covers all three application surfaces:

- Backend compilation and relationship isolation tests.
- Web frontend production build.
- Mobile dependency installation and TypeScript type check.

The workflow also uses `actions/checkout@v5` to avoid the previous checkout Node 20 deprecation warning.

## Remaining work

### Web production hardening
- Select the production hosting provider.
- Add only the provider-specific SPA/deployment configuration required by that host.
- Configure production `VITE_API_URL`, backend CORS and `FRONTEND_URL`.
- Perform a clean PostgreSQL + Flyway deployment test.
- Test tenant isolation with multiple tenants.
- Complete full admin → class → ClassSubject → teacher → student end-to-end testing.
- Review npm audit findings and upgrade dependencies safely.
- Add production monitoring, logging and database backup/restore procedures.
- Perform final security review.

### AI timetable import
- Make multi-entry confirmation transactional/batch validated so a later conflict cannot leave earlier imported slots saved.
- Add automated tests for import parsing, invalid candidates and conflict handling.
- Test PDF/image imports with real timetable samples.

### Mobile release hardening
- Add teacher attendance/academic-management mobile workflows beyond the current dashboard foundation.
- Add admin/super-admin operational management screens where appropriate.
- Add password change and forgot-password flows.
- Improve offline handling and user-facing session-expiry navigation.
- Add push notifications if required.
- Configure final Android/iOS icons, splash assets and release metadata.
- Test on physical Android/iOS devices.
- Create signed Android/iOS release builds and publish through the selected distribution channel.

## Important architecture rule

The mobile app must not create a second backend or duplicate business logic. It uses the same Spring Boot API and the same tenant/authentication model as the web application.

Operational academic relationships remain:

```text
Student
  ↓
ClassEnrollment
  ↓
ClassSubject
  ├── Class
  ├── Subject
  └── Teacher
       ├── Attendance
       ├── Assignment
       ├── Exam
       ├── Marks
       └── Timetable
```
