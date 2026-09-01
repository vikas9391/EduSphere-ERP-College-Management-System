package com.collegeerp.Backend.teacher.service;

import com.collegeerp.Backend.assignment.entity.Assignment;
import com.collegeerp.Backend.assignment.entity.AssignmentSubmission;
import com.collegeerp.Backend.assignment.repository.AssignmentRepository;
import com.collegeerp.Backend.assignment.repository.AssignmentSubmissionRepository;
import com.collegeerp.Backend.attendance.entity.Attendance;
import com.collegeerp.Backend.attendance.repository.AttendanceRepository;
import com.collegeerp.Backend.attendance.service.AttendanceStatusPolicy;
import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.UserRepository;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.schoolclass.repository.ClassSubjectRepository;
import com.collegeerp.Backend.teacher.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Aggregates teacher dashboard data from authoritative class relationships. */
@Service
@Transactional(readOnly = true)
public class TeacherDashboardService {

    private static final Logger log = LoggerFactory.getLogger(TeacherDashboardService.class);
    private static final String EVALUATED_STATUS = "EVALUATED";

    private final UserRepository userRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final TeacherScheduleService scheduleService;
    private final TeacherAnnouncementService announcementService;

    public TeacherDashboardService(
            UserRepository userRepository,
            ClassSubjectRepository classSubjectRepository,
            ClassEnrollmentRepository classEnrollmentRepository,
            AttendanceRepository attendanceRepository,
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            TeacherScheduleService scheduleService,
            TeacherAnnouncementService announcementService) {
        this.userRepository = userRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.scheduleService = scheduleService;
        this.announcementService = announcementService;
    }

    public TeacherDashboardResponse getDashboard(Long teacherId) {
        log.debug("Building class-scoped dashboard for teacher id={}", teacherId);

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> ResourceNotFoundException.of("Teacher", teacherId));

        List<ClassSubject> classSubjects = classSubjectRepository.findAllByTeacherId(teacherId);
        List<ClassEnrollment> classEnrollments = classEnrollmentRepository.findAllByTeacherId(teacherId);
        long totalStudents = classEnrollments.stream()
                .map(e -> e.getStudent().getId())
                .distinct()
                .count();

        List<Assignment> assignments = assignmentRepository.findByTeacherId(teacherId);
        List<Attendance> attendanceRecords = attendanceRepository.findClassAttendanceByTeacherId(teacherId);
        List<TeacherScheduleEntryResponse> todaysSchedule = scheduleService.getTodaysSchedule(teacherId);

        return TeacherDashboardResponse.builder()
                .teacherId(teacher.getId())
                .teacherName(teacher.getFirstName() + " " + (teacher.getLastName() != null ? teacher.getLastName() : ""))
                .totalSubjects(classSubjects.size())
                .totalStudents((int) totalStudents)
                .pendingReviewCount(countPendingReview(assignments))
                .attendancePendingToday(countAttendancePendingToday(classSubjects, classEnrollments, attendanceRecords))
                .upcomingClassesCount(todaysSchedule.size())
                .assignmentsPerSubject(assignmentsPerSubject(assignments))
                .attendanceTrend(attendanceTrend(attendanceRecords))
                .recentAssignments(recentAssignments(assignments))
                .todaysSchedule(todaysSchedule)
                .schedulePlaceholder(false)
                .announcements(announcementService.getAnnouncements())
                .announcementsPlaceholder(true)
                .build();
    }

    private int countPendingReview(List<Assignment> assignments) {
        if (assignments.isEmpty()) return 0;
        List<Long> assignmentIds = assignments.stream().map(Assignment::getId).toList();
        List<AssignmentSubmission> submissions = submissionRepository.findByAssignmentIdIn(assignmentIds);
        return (int) submissions.stream()
                .filter(s -> !EVALUATED_STATUS.equalsIgnoreCase(s.getStatus()))
                .count();
    }

    /** Only class subjects with an actual student roster require attendance today. */
    private int countAttendancePendingToday(
            List<ClassSubject> classSubjects,
            List<ClassEnrollment> classEnrollments,
            List<Attendance> attendanceRecords) {
        LocalDate today = LocalDate.now();
        Set<Long> activeClassSubjectIds = classEnrollments.stream()
                .map(e -> e.getClassSubject().getId())
                .collect(Collectors.toSet());
        Set<Long> markedToday = attendanceRecords.stream()
                .filter(a -> a.getAttendanceDate() != null && a.getAttendanceDate().isEqual(today))
                .filter(a -> a.getClassEnrollment() != null && a.getClassEnrollment().getClassSubject() != null)
                .map(a -> a.getClassEnrollment().getClassSubject().getId())
                .collect(Collectors.toSet());
        return (int) classSubjects.stream()
                .map(ClassSubject::getId)
                .filter(activeClassSubjectIds::contains)
                .filter(id -> !markedToday.contains(id))
                .count();
    }

    private List<SubjectAssignmentCountResponse> assignmentsPerSubject(List<Assignment> assignments) {
        Map<String, Long> counts = assignments.stream()
                .collect(Collectors.groupingBy(a -> a.getClassSubject() != null
                        ? a.getClassSubject().getSubjectName()
                        : a.getSubject().getSubjectName(), Collectors.counting()));
        return counts.entrySet().stream()
                .map(entry -> SubjectAssignmentCountResponse.builder()
                        .subjectName(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();
    }

    /** Last seven days using the same PRESENT/LATE/EXCUSED policy as student attendance. */
    private List<AttendanceTrendPointResponse> attendanceTrend(List<Attendance> attendanceRecords) {
        Map<LocalDate, List<Attendance>> byDate = attendanceRecords.stream()
                .filter(a -> a.getAttendanceDate() != null)
                .collect(Collectors.groupingBy(Attendance::getAttendanceDate));

        List<AttendanceTrendPointResponse> trend = new java.util.ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            List<Attendance> dayRecords = byDate.getOrDefault(date, List.of());
            long denominator = dayRecords.stream()
                    .filter(a -> AttendanceStatusPolicy.countsTowardPercentage(a.getStatus()))
                    .count();
            long attended = dayRecords.stream()
                    .filter(a -> AttendanceStatusPolicy.countsTowardPercentage(a.getStatus()))
                    .filter(a -> AttendanceStatusPolicy.isAttended(a.getStatus()))
                    .count();
            int rate = denominator == 0 ? 0 : (int) Math.round((attended * 100.0) / denominator);
            trend.add(AttendanceTrendPointResponse.builder()
                    .label(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .ratePercentage(rate)
                    .build());
        }
        return trend;
    }

    private List<TeacherAssignmentResponse> recentAssignments(List<Assignment> assignments) {
        return assignments.stream()
                .sorted(Comparator.comparing(Assignment::getDueDate).reversed())
                .limit(5)
                .map(a -> TeacherAssignmentResponse.builder()
                        .assignmentId(a.getId())
                        .title(a.getTitle())
                        .subjectId(a.getSubject().getId())
                        .subjectName(a.getClassSubject() != null
                                ? a.getClassSubject().getSubjectName()
                                : a.getSubject().getSubjectName())
                        .dueDate(a.getDueDate())
                        .maxMarks(a.getMaxMarks())
                        .build())
                .toList();
    }
}
