# EduSphere ERP — Project Status

Last updated: 2026-09-15

## Current position

EduSphere has completed the main ERP architecture and the migration from the old standalone enrollment model to a class-scoped operational model.

The current priority is **integration, deployment hardening, security verification and end-to-end validation**.

## Architecture completed

The authoritative academic chain is:

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

The old standalone `Enrollment` operational path is no longer part of the design.

## Completed areas

### 1. Authentication and security

- JWT access/refresh authentication.
- Password change and forgot-password/reset flows.
- Staff role/permission controls.
- Backend authorization for tenant/staff/teacher/student operations.
- Frontend role guards for protected dashboards and routes.
- Student identity resolution through `User → Student`, without assuming matching IDs.

### 2. Multi-tenancy

- Schema-based PostgreSQL tenancy.
- Public tenant metadata separated from tenant schemas.
- Public and tenant Flyway migrations.
- Hibernate DDL generation disabled so Flyway owns schema creation.

### 3. Class management

- School class creation and deletion.
- Class student membership through `ClassStudent`.
- Add/remove students from classes.
- ClassSubject creation and bulk creation.
- Teacher assignment to ClassSubjects.
- Optional formal Subject/curriculum association.
- Class roster and class enrollment views.

### 4. ClassEnrollment model

`ClassEnrollment` is the authoritative record connecting a student to a taught ClassSubject.

It carries the class, subject, teacher and student context required by operational academic features.

### 5. Attendance

- Attendance is stored against `ClassEnrollment`.
- Teacher access is restricted to their assigned ClassSubjects.
- Students can view only their own records.
- Admin/super-admin management is supported.
- Unique attendance per ClassEnrollment/date.
- Attendance summaries and subject-wise calculations.
- Status policy supports `PRESENT`, `ABSENT`, `LATE`, `EXCUSED` and `HOLIDAY`.
- `EXCUSED` and `HOLIDAY` are excluded from the percentage denominator.
- Class holiday calendar API and student holiday display are implemented.
- Old standalone enrollment/attendance compatibility code was removed.

### 6. Assignments

- Assignment ownership is through ClassSubject.
- Teacher ownership is derived from ClassSubject.teacher.
- Student assignment visibility is based on ClassEnrollment/ClassSubject membership.
- Assignment submission is tied to the student's ClassEnrollment.
- Teacher/admin review is class-scoped.

### 7. Examinations, marks and results

- Exam schedules are class-subject scoped.
- Marks are class-enrollment scoped.
- Eligibility and result paths use the class-scoped relationship.
- Teacher access is restricted through ClassSubject ownership.
- Legacy subject-only enrollment fallback was removed from the operational design.

### 8. Timetable

- TimetableEntry is derived from ClassSubject.
- Student timetable follows ClassEnrollment → ClassSubject → TimetableEntry.
- Teacher timetable follows the teacher's ClassSubjects.
- Class and teacher conflict checks are implemented.
- Create/update/delete operations are implemented.
- Flexible teacher timetable grid supports visible/hidden days, custom rows and period/slot creation.
- ClassSubjects without a teacher now produce a validation error instead of an unsafe null access.

### 9. AI timetable import

Implemented flow:

```text
Image/PDF upload
      ↓
Backend inspection
      ↓
OpenAI vision/file processing
      ↓
Normalized candidates
      ↓
Admin/teacher review and editing
      ↓
Existing conflict validation
      ↓
Save
```

The OpenAI API key is backend-only. Supported import input is image/PDF within the configured upload limit.

### 10. Frontend API and routing

- Central Axios client.
- Environment-driven `VITE_API_URL`.
- Access token injection.
- Refresh-token retry queue.
- Response envelope handling.
- Protected role-based routing.
- Student, teacher and admin portal routes aligned with the current backend model.

### 11. Database cleanup

Obsolete standalone enrollment migrations and old attendance/enrollment compatibility paths were removed from the fresh-database model.

The project intentionally assumes a clean database reset when adopting the current migration set.

### 12. CI

Latest verified CI before this documentation update:

- Backend build: PASS.
- Relationship tests: **9 passed, 0 failed, 0 errors**.
- Frontend production build: PASS.

The relationship test suite covers attendance, assignment submission, marks class scope, results and timetable.

## Remaining work

### Priority 1 — production deployment

1. Select the production hosting provider.
2. Configure frontend build/output for that provider.
3. Configure backend runtime for Java 21 and port 8080.
4. Configure production PostgreSQL and Redis as required.
5. Set `VITE_API_URL`, `FRONTEND_URL` and `CORS_ALLOWED_ORIGINS` to the real domains.
6. Set a strong production `JWT_SECRET`.
7. Configure AI and mail secrets only if those features are enabled.
8. Verify SPA fallback behavior for direct React routes.

Provider-specific files have intentionally not been added yet because the hosting target is not defined.

### Priority 2 — clean deployment validation

- Start the backend against a clean database.
- Run public and tenant Flyway migrations.
- Create a tenant and verify its schema.
- Verify login, refresh and password reset.
- Create a class and ClassSubjects.
- Assign teachers and students.
- Verify attendance, holidays, assignments, submissions, exams, marks, results and timetables.
- Test student and teacher authorization boundaries.
- Test tenant isolation using two tenants.

### Priority 3 — timetable import robustness

The current review UI saves imported candidates one by one. If an early candidate succeeds and a later candidate fails conflict validation, the earlier candidates may already be saved.

Recommended next implementation:

```text
Review all candidates
      ↓
Backend validates the complete batch
      ↓
If every candidate is valid → save all in one transaction
If any candidate is invalid → save none and return errors
```

This should be completed before relying on AI import for large production timetable uploads.

### Priority 4 — dependency and CI hygiene

- Review the currently reported frontend npm audit findings.
- Upgrade only compatible dependencies and rerun the frontend build.
- Optionally update `actions/checkout` to its current major version to remove the Node 20 deprecation warning.

### Priority 5 — operations/security

- Configure production database backups and restore testing.
- Add production monitoring and alerting.
- Review public actuator/API exposure.
- Perform a final CORS/JWT/file-upload/security review.
- Confirm production secrets are managed outside Git.

## Definition of production-ready

The project should be considered production-ready when:

- [ ] A real hosting target is configured.
- [ ] Clean database deployment succeeds.
- [ ] Tenant creation and isolation are verified.
- [ ] All critical role flows pass end-to-end testing.
- [ ] Production frontend/backend domains and CORS are verified.
- [ ] Backups and monitoring are configured.
- [ ] Security review is complete.
- [ ] AI timetable import is transactional or otherwise protected against partial saves, if the feature is enabled.
- [ ] Frontend dependency audit has been reviewed.
- [ ] CI remains green after final deployment changes.

## Important distinction

The remaining checklist is primarily **deployment and production hardening**. It should not be interpreted as saying the core ERP relationship architecture is incomplete.
