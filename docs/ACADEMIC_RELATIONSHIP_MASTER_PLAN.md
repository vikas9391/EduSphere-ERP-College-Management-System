# EduSphere ERP — Academic Relationship Master Plan

> Architecture baseline and implementation plan for the Student, Teacher, Class, Subject, Enrollment and Attendance model.

## 1. Current problem

The repository currently contains two overlapping student-subject paths: Enrollment → Subject and ClassEnrollment → ClassSubject → Subject. Attendance also supports both paths. This can make student attendance and enrollment data disagree even when individual APIs work.

The recent student attendance error, “Failed to load your attendance. Please try again.”, should therefore be treated as a relationship/data-contract problem rather than only a frontend display problem.

Announcements also established an identity rule: authenticated User ID must not be assumed to equal Student ID. Student operations must resolve User → Student → Student.id.

## 2. Target architecture

### Curriculum
Department → Course → Subject

These are master/curriculum records.

### Teaching
Teacher → SchoolClass
SchoolClass → ClassStudent → Student
SchoolClass → ClassSubject → Subject + Teacher

ClassStudent means the student belongs to the class. ClassSubject means the subject is taught in that class and identifies the teacher who teaches it.

### Student participation
ClassSubject → ClassEnrollment → Student

ClassEnrollment is the authoritative operational student-subject relationship.

### Operational records
ClassEnrollment should ultimately be the source for Attendance, assignment visibility/submission, exam participation and marks/results.

## 3. Exact relationship rules

| Entity | Relationship | Meaning |
|---|---|---|
| Department | Course | Department owns courses |
| Course | Subject | Course curriculum contains subjects |
| Course | Student | Student belongs to a course |
| Teacher | SchoolClass | Teacher may be class teacher/owner |
| SchoolClass | ClassStudent | Student belongs to class |
| SchoolClass | ClassSubject | Subject is offered in class |
| ClassSubject | Subject | Formal curriculum subject |
| ClassSubject | Teacher | Teacher teaches that class subject |
| ClassEnrollment | Student | Student takes the class subject |
| ClassEnrollment | ClassSubject | Exact taught subject instance |
| Attendance | ClassEnrollment | Attendance for that participation |

## 4. Teacher model

Do not merge class teacher and subject teacher.

Example: BCT-2026-A can have Teacher A as class teacher while Blockchain is taught by Teacher B and Java by Teacher C.

Authorization must use the relevant relationship. A class teacher must not automatically be allowed to edit every subject unless the product explicitly grants that permission.

## 5. Student identity

Every student-facing backend operation should follow:

Authenticated User → Student profile → Student.id → academic data

Create one reusable backend resolver for this. Do not duplicate User-ID/email resolution in every service.

## 6. Enrollment policy

Mandatory subjects: when a student joins a class, active mandatory ClassSubjects create ClassEnrollments automatically.

Electives: the student chooses an available ClassSubject and the backend creates a validated ClassEnrollment.

Reject enrollment when the student is not a member of the class, the subject is unavailable, or an active duplicate enrollment already exists.

Recommended uniqueness: student_id + class_subject_id.

## 7. Attendance policy

Final target: Attendance → ClassEnrollment.

The current Attendance entity still supports both enrollment_id and class_enrollment_id. Keep this temporarily for migration/compatibility; do not delete it until existing data is reconciled.

The student attendance response must contain overall percentage, total classes, attended, missed, and one row for every current ClassEnrollment subject.

A subject with zero attendance records must still appear with zero totals.

Canonical statuses should be PRESENT, ABSENT, LATE and EXCUSED. The attendance denominator rule for EXCUSED must be explicitly defined and used consistently everywhere.

## 8. Attendance implementation requirement

Do not use find-all-and-filter as the long-term student/teacher query strategy. Add repository queries scoped by Student, ClassEnrollment, ClassSubject and Teacher so filtering and authorization happen efficiently in the database.

## 9. Student Enrollments

Enrolled Subjects must come from active ClassEnrollments.

Available Subjects must come from ClassSubjects for the student's current class, academic year and semester, filtered for active/elective availability and existing enrollment.

Do not use the complete Course curriculum as the student's current enrollment.

## 10. My Classes

My Classes must be derived from ClassStudent. Subjects and teachers inside a class must come from ClassSubject.

## 11. Assignments

Target: ClassSubject → Assignment → applicable ClassEnrollment → Student.

This prevents an assignment created for one class from appearing for every student taking the same formal Subject elsewhere.

## 12. Exams and marks

Target: ClassSubject → ExamSchedule → ClassEnrollment → Student → Marks.

The same participation model should drive result calculations.

## 13. Announcements

Direct student recipients must use Student.id.

Authenticated student lookup must be User → Student → Student.id.

Class announcements should resolve through Class → ClassStudent → Student.

## 14. Migration strategy

Do not delete Enrollment or old attendance fields first.

### Phase 0 — Safety
- Back up the database.
- Record tenant schemas and important row counts.
- Record existing student/class/subject/enrollment/attendance relationships.

### Phase 1 — Audit
- Find every use of Enrollment and ClassEnrollment.
- Find every use of Subject and ClassSubject.
- Find all student identity resolution code.
- Find all student and teacher APIs and frontend consumers.

### Phase 2 — Mapping
Map existing Enrollment records to ClassSubject/ClassEnrollment where the relationship is unambiguous. Unmatched records must be reported, not silently deleted.

### Phase 3 — Migration
Create missing ClassEnrollments where safe and migrate attendance to the corresponding ClassEnrollment. Keep compatibility columns during verification.

### Phase 4 — Verification gate
Do not continue until attendance counts, student subject counts, class rosters, teacher ownership and sample student reports reconcile.

### Phase 5 — Cutover
New operational attendance, assignments, exams and marks use the ClassEnrollment/ClassSubject model.

### Phase 6 — Legacy cleanup
Only after production verification, stop writing legacy operational records and remove compatibility code/columns through explicit migrations.

## 15. API rule

Student self-service APIs should identify the student from authentication rather than trusting a client-supplied Student ID.

Prefer stable endpoints such as /student/classes, /student/enrollments, /student/attendance/summary, /student/assignments and /student/results.

The frontend should receive one authoritative response rather than merging contradictory backend sources.

## 16. Frontend rule

Compatibility fallback code is acceptable only during migration. The final student pages must not merge formal Enrollment data with ClassSubject data merely to hide backend inconsistencies.

Attendance fallback logic should also be removed after the backend contract is stable.

## 17. Security

Students must never be able to read or modify another student's records.

Teachers must be authorized against the actual ClassSubject they teach.

Enrollment, attendance, assignments, exams and marks must all validate the authenticated user's relationship to the relevant academic record.

## 18. Database constraints

Where compatible with existing data, enforce unique class membership, unique active student/ClassSubject enrollment, and unique attendance per ClassEnrollment/date.

Do not add constraints until existing duplicates have been identified and cleaned.

## 19. Controlled integration test

Use a small test dataset:

Department: Computer Science
Course: BCT
Class: BCT-2026-A
Students: Vikas, Rahul
Subjects: Blockchain, Java
Teachers: Teacher A, Teacher B

Expected teaching relationships:
Class Teacher → Teacher A
Blockchain → Teacher B
Java → Teacher A

For Vikas:
Blockchain: 10 total, 8 attended, 2 missed
Java: 10 total, 9 attended, 1 missed
Overall: 20 total, 17 attended, 3 missed, 85%

The same subject relationships must appear consistently in My Classes, My Enrollments, My Attendance and Teacher views.

## 20. Acceptance checklist

- [ ] Student identity resolves consistently from User → Student.
- [ ] ClassStudent represents class membership.
- [ ] ClassSubject represents subject + class + teacher.
- [ ] ClassEnrollment represents actual student participation.
- [ ] Attendance uses ClassEnrollment.
- [ ] My Enrollments uses active ClassEnrollments.
- [ ] My Classes uses ClassStudent/ClassSubject.
- [ ] Assignments use ClassSubject.
- [ ] Exams/results use the same participation model.
- [ ] Announcements use Student IDs correctly.
- [ ] Mandatory enrollment is automatic.
- [ ] Elective enrollment is validated.
- [ ] Duplicate relationships are prevented.
- [ ] Existing tenant data is reconciled before legacy cleanup.
- [ ] Student, teacher and admin integration tests pass.

## 21. Implementation order

1. Full entity/repository/service audit
2. Final relationship map
3. Central Student identity resolver
4. Class/ClassStudent/ClassSubject/ClassEnrollment validation
5. Enrollment compatibility and migration
6. Attendance migration and summary API
7. Student enrollment and class APIs
8. Assignments
9. Exams and marks
10. Announcements
11. Student frontend cleanup
12. Teacher frontend cleanup
13. Admin validation
14. Full integration testing
15. Legacy cleanup

Each phase should compile and pass its relevant tests before the next phase begins.

## 22. Final architectural rule

Department and Course define the curriculum. Subject defines a curriculum subject. SchoolClass defines the teaching group. ClassStudent defines class membership. ClassSubject defines the subject taught and its teacher. ClassEnrollment defines the student's actual participation. Operational academic records follow ClassEnrollment.

**This document is the master plan. Existing data must be preserved until the migration is verified.**