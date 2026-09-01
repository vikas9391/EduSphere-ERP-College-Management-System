package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.assignment.entity.Assignment;
import com.collegeerp.Backend.assignment.repository.AssignmentRepository;
import com.collegeerp.Backend.assignment.repository.AssignmentSubmissionRepository;
import com.collegeerp.Backend.attendance.repository.AttendanceRepository;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.examination.repository.ExamScheduleRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.entity.ClassStudent;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.schoolclass.repository.ClassStudentRepository;
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
 * Student dashboard reads operational student participation from ClassStudent/ClassEnrollment.
 * Legacy Enrollment is intentionally not used to derive subject counts or attendance metrics.
 */
@Service
@Transactional(readOnly = true)
public class StudentDashboardService {

    private static final Logger log = LoggerFactory.getLogger(StudentDashboardService.class);

    private final StudentRepository studentRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final StudentResultService studentResultService;
    private final StudentNotificationService studentNotificationService;

    public StudentDashboardService(
            StudentRepository studentRepository,
            ClassStudentRepository classStudentRepository,
            ClassEnrollmentRepository classEnrollmentRepository,
            AttendanceRepository attendanceRepository,
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            ExamScheduleRepository examScheduleRepository,
            StudentResultService studentResultService,
            StudentNotificationService studentNotificationService) {

        this.studentRepository = studentRepository;
        this.classStudentRepository = classStudentRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
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

        List<ClassStudent> memberships = classStudentRepository.findAllByStudentId(studentId);
        List<ClassEnrollment> classEnrollments = classEnrollmentRepository.findAllByStudentId(studentId);
        List<Subject> subjects = classEnrollments.stream()
                .map(ClassEnrollment::getClassSubject)
                .filter(java.util.Objects::nonNull)
                .map(cs -> cs.getSubject())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        return StudentDashboardResponse.builder()
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + (student.getLastName() != null ? student.getLastName() : ""))
                .rollNumber(student.getRollNumber())
                .department(resolveDepartment(student, subjects))
                .course(resolveCourse(student, subjects))
                .semester(resolveSemester(classEnrollments))
                .cgpa(studentResultService.getResults(studentId).getCgpa())
                .attendancePercentage(attendancePercentage(studentId))
                .totalSubjects(subjects.size())
                .pendingAssignments(countPendingAssignments(studentId, subjects))
                .upcomingExams(countUpcomingExams(subjects))
                .notificationsCount((int) studentNotificationService.getUnreadCount(studentId))
                .build();
    }

    private String resolveDepartment(Student student, List<Subject> subjects) {
        if (student.getCourse() != null && student.getCourse().getDepartment() != null) {
            return student.getCourse().getDepartment().getName();
        }
        return subjects.stream()
                .map(Subject::getCourse)
                .filter(java.util.Objects::nonNull)
                .map(course -> course.getDepartment())
                .filter(java.util.Objects::nonNull)
                .map(department -> department.getName())
                .findFirst()
                .orElse(null);
    }

    private String resolveCourse(Student student, List<Subject> subjects) {
        if (student.getCourse() != null) {
            return student.getCourse().getCourseName();
        }
        return subjects.stream()
                .map(Subject::getCourse)
                .filter(java.util.Objects::nonNull)
                .map(course -> course.getCourseName())
                .findFirst()
                .orElse(null);
    }

    private Integer resolveSemester(List<ClassEnrollment> classEnrollments) {
        return classEnrollments.stream()
                .map(ClassEnrollment::getClassSubject)
                .filter(java.util.Objects::nonNull)
                .map(cs -> cs.getSchoolClass())
                .filter(java.util.Objects::nonNull)
                .map(sc -> sc.getSemester())
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private double attendancePercentage(Long studentId) {
        List<?> records = attendanceRepository.findClassAttendanceByStudentId(studentId);
        if (records.isEmpty()) {
            return 0.0;
        }
        long attended = records.stream()
                .map(r -> (com.collegeerp.Backend.attendance.entity.Attendance) r)
                .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()))
                .count();
        return Math.round((attended * 10000.0) / records.size()) / 100.0;
    }

    private int countPendingAssignments(Long studentId, List<Subject> subjects) {
        List<Long> subjectIds = subjects.stream().map(Subject::getId).toList();
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

    private int countUpcomingExams(List<Subject> subjects) {
        List<Long> subjectIds = subjects.stream().map(Subject::getId).toList();
        if (subjectIds.isEmpty()) {
            return 0;
        }
        return examScheduleRepository.findUpcomingBySubjectIds(subjectIds, LocalDate.now()).size();
    }
}
