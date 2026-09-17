# EduSphere ERP — Project Status

## Current status

The core ERP architecture is implemented around a class-scoped academic model. The repository contains:

- `Backend/` — Spring Boot 3.5.3 / Java 21 REST API.
- `frontend-web/` — React 19 + TypeScript + Vite web application.
- `app/` — Flutter/Dart mobile application using the same backend.

The former Expo/React Native mobile implementation has been removed.

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

### Flutter mobile app
The `app/` directory is now the Flutter/Dart client backed by the same Spring Boot API.

Implemented:
- Flutter/Dart project structure.
- Material 3 EduSphere green/white branding.
- College code, username and password login.
- Persistent access/refresh-token session storage with `shared_preferences`.
- Automatic access-token refresh on protected API `401` responses, with session cleanup when refresh fails.
- Role-aware initial dashboard routing for student, teacher and admin accounts.
- Student and teacher dashboard data integration.
- Logout flow.
- Backend REST integration through `http`.
- Flutter static analysis in CI.

Removed:
- Expo configuration.
- React Native source files.
- React Navigation mobile source.
- TypeScript mobile source.
- npm mobile package configuration and mobile TypeScript configuration.
- Expo environment configuration.

## CI verification

The CI workflow covers all three application surfaces:

- Backend compilation and relationship isolation tests.
- Web frontend production build.
- Flutter dependency installation and `flutter analyze`.

The mobile job no longer installs Node/Expo dependencies or runs a TypeScript type check.

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

### Flutter mobile release hardening
- Build all Student modules: classes, attendance, assignments, timetable, exams, marks and results.
- Build all Teacher modules: classes/rosters, attendance, assignments, timetable, exams and marks.
- Add appropriate Admin/Super-admin operational screens.
- Add profile, password change and forgot-password flows.
- Improve offline handling and user-facing session-expiry navigation.
- Add push notifications if required.
- Configure final Android/iOS icons, splash assets and release metadata.
- Test on physical Android/iOS devices.
- Create signed Android/iOS release builds and publish through the selected distribution channel.

## Important architecture rule

The mobile app must not create a second backend or duplicate business logic. It uses the same Spring Boot API and the same tenant/authentication model as the web application.

The mobile stack is Flutter/Dart. Do not reintroduce Expo, React Native, React Navigation, TypeScript mobile source or npm-based mobile configuration.

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
