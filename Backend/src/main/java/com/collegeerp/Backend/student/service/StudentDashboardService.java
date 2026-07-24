package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.assignment.entity.Assignment;
import com.collegeerp.Backend.assignment.repository.AssignmentRepository;
import com.collegeerp.Backend.assignment.repository.AssignmentSubmissionRepository;
import com.collegeerp.Backend.attendance.repository.AttendanceRepository;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.enrollment.entity.Enrollment;
import com.collegeerp.Backend.enrollment.repository.EnrollmentRepository;
import com.collegeerp.Backend.examination.repository.ExamScheduleRepository;
import com.collegeerp.Backend.student.dto.StudentDashboardResponse;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import com.collegeerp.Backend.subject.entity.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Aggregates data already owned by other packages ({@code enrollment}, {@code attendance},
 * {@code assignment}, {@code result}/{@code marks} via {@link StudentResultService},
 * {@code examination}, plus the placeholder {@link StudentNotificationService}) into a single
 * dashboard summary for the logged-in student. Deliberately does not duplicate any of those
 * packages' business logic - it only reads and combines.
 */
@Service
@Transactional(readOnly = true)
public class StudentDashboardService {

    private static final Logger log = LoggerFactory.getLogger(StudentDashboardService.class);

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final StudentResultService studentResultService;
    private final StudentNotificationService studentNotificationService;

    public StudentDashboardService(
            StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository,
            AttendanceRepository attendanceRepository,
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            ExamScheduleRepository examScheduleRepository,
            StudentResultService studentResultService,
            StudentNotificationService studentNotificationService) {

        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.studentResultService = studentResultService;
        this.studentNotificationService = studentNotificationService;
    }

    public StudentDashboardResponse getDashboard(Long studentId) {

        log.debug("Building dashboard for student id={}", studentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Student", studentId));

        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdWithDetails(studentId);

        List<Long> subjectIds = enrollments.stream()
                .map(e -> e.getSubject().getId())
                .distinct()
                .toList();

        return StudentDashboardResponse.builder()
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + (student.getLastName() != null ? student.getLastName() : ""))
                .rollNumber(student.getRollNumber())
                .department(resolveDepartment(enrollments))
                .course(resolveCourse(enrollments))
                .semester(resolveSemester(enrollments))
                .cgpa(studentResultService.getResults(studentId).getCgpa())
                .attendancePercentage(attendancePercentage(studentId))
                .totalSubjects(subjectIds.size())
                .pendingAssignments(countPendingAssignments(studentId, subjectIds))
                .upcomingExams(countUpcomingExams(subjectIds))
                .notificationsCount((int) studentNotificationService.getUnreadCount(studentId))
                .build();
    }

    /**
     * The schema has no direct Student -> Department FK; a student's department is only ever
     * derivable through their subject enrollments (Subject -> Course -> Department). This takes
     * the most recently enrolled subject's department as the student's "current" department.
     * Returns null if the student has no enrollments yet.
     */
    private String resolveDepartment(List<Enrollment> enrollments) {
        return mostRecentSubject(enrollments)
                .map(Subject::getCourse)
                .map(course -> course.getDepartment())
                .map(department -> department.getName())
                .orElse(null);
    }

    /** Same derivation rationale as {@link #resolveDepartment}, via the course instead. */
    private String resolveCourse(List<Enrollment> enrollments) {
        return mostRecentSubject(enrollments)
                .map(Subject::getCourse)
                .map(course -> course.getCourseName())
                .orElse(null);
    }

    /** Taken from the most recent enrollment's own semester field, not the subject's. */
    private Integer resolveSemester(List<Enrollment> enrollments) {
        return enrollments.stream()
                .max(Comparator.comparing(Enrollment::getEnrollmentDate))
                .map(Enrollment::getSemester)
                .orElse(null);
    }

    private java.util.Optional<Subject> mostRecentSubject(List<Enrollment> enrollments) {
        return enrollments.stream()
                .max(Comparator.comparing(Enrollment::getEnrollmentDate))
                .map(Enrollment::getSubject);
    }

    private double attendancePercentage(Long studentId) {
        var records = attendanceRepository.findByStudentId(studentId);
        if (records.isEmpty()) {
            return 0.0;
        }
        long attended = records.stream()
                .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()))
                .count();
        return Math.round((attended * 10000.0) / records.size()) / 100.0;
    }

    /** An assignment is "pending" for this dashboard if the student has not submitted it yet. */
    private int countPendingAssignments(Long studentId, List<Long> subjectIds) {
        if (subjectIds.isEmpty()) {
            return 0;
        }
        List<Assignment> assignments = assignmentRepository.findBySubjectIdIn(subjectIds);
        Set<Long> submittedAssignmentIds = submissionRepository.findByStudentId(studentId).stream()
                .map(s -> s.getAssignment().getId())
                .collect(java.util.stream.Collectors.toSet());

        return (int) assignments.stream()
                .filter(a -> !submittedAssignmentIds.contains(a.getId()))
                .count();
    }

    private int countUpcomingExams(List<Long> subjectIds) {
        if (subjectIds.isEmpty()) {
            return 0;
        }
        return examScheduleRepository.findUpcomingBySubjectIds(subjectIds, LocalDate.now()).size();
    }
}
