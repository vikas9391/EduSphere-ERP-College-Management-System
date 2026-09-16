# EduSphere ERP — Project Status

## Current status

The core ERP architecture is implemented around a class-scoped academic model. The repository contains:

- `Backend/` — Spring Boot 3.5.3 / Java 21 REST API.
- `frontend-web/` — React 19 + TypeScript + Vite web application.
- `app/` — Expo/React Native mobile application using the same backend and the web application's botanical color system.

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
- Student profile, classes, enrollments, attendance, holidays, assignments and timetable views.
- Teacher academic management views.
- Class roster and ClassSubject management.
- Flexible teacher timetable grid with selectable days, custom rows, add/edit/delete controls.
- AI timetable upload/review UI for image/PDF imports.
- Central API client with environment-driven `VITE_API_URL`.
- Access-token and refresh-token handling.
- Shared botanical design system using green/white/soft-neutral surfaces, rounded cards and responsive layouts.

### Mobile app
The `app/` directory has now been created as the mobile application foundation.

Implemented in the first mobile milestone:

- Expo + React Native project structure.
- Same Spring Boot backend login endpoint: `/api/auth/login`.
- Secure session persistence using AsyncStorage for the current foundation.
- College code, username and password login form.
- Post-login dashboard shell.
- Sign-out flow.
- Shared botanical color tokens matching the web application:
  - Primary `#2e7d32`
  - Secondary `#4caf50`
  - Light green `#e8f5e9`
  - Background `#f8f8f2`
  - Card `#ffffff`
  - Text `#1f2937`
  - Muted `#6b7280`
- Mobile environment variable for the backend API URL.

## CI verification

The latest verified CI run before the mobile scaffold was green:

- Backend compilation: PASS.
- Relationship tests: 9 tests, 0 failures, 0 errors.
- Frontend production build: PASS.

The mobile folder was added after that CI run, so a follow-up CI job for mobile installation/type checking should be added before treating the mobile app as release-ready.

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

### Mobile app — next milestones
1. Add React Navigation and role-specific navigation stacks.
2. Build Student dashboard and modules using the existing APIs.
3. Build Teacher dashboard and modules.
4. Add Admin/Super-admin mobile views where appropriate.
5. Add attendance views and teacher attendance management.
6. Add assignments and submission views.
7. Add student/teacher timetable views.
8. Add exams, marks and results views.
9. Add profile, password change and forgot-password flows.
10. Add API refresh-token interceptor and robust offline/error handling.
11. Add mobile push notifications if required.
12. Add automated mobile type-check/build CI.
13. Configure Android/iOS application icons, splash assets and release metadata.
14. Test on physical Android/iOS devices.
15. Create signed Android/iOS release builds.

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
