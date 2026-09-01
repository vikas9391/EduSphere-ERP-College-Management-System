package com.collegeerp.Backend.teacher.service;

import com.collegeerp.Backend.assignment.entity.Assignment;
import com.collegeerp.Backend.assignment.entity.AssignmentSubmission;
import com.collegeerp.Backend.assignment.repository.AssignmentRepository;
import com.collegeerp.Backend.assignment.repository.AssignmentSubmissionRepository;
import com.collegeerp.Backend.attendance.entity.Attendance;
import com.collegeerp.Backend.attendance.repository.AttendanceRepository;
import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.UserRepository;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.enrollment.entity.Enrollment;
import com.collegeerp.Backend.enrollment.repository.EnrollmentRepository;
import com.collegeerp.Backend.subject.entity.Subject;
import com.collegeerp.Backend.subject.repository.SubjectRepository;
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
import java.util.stream.Collectors;

/** Aggregates teacher dashboard data from operational repositories. */
@Service
@Transactional(readOnly = true)
public class TeacherDashboardService {

    private static final Logger log = LoggerFactory.getLogger(TeacherDashboardService.class);
    private static final String PRESENT_STATUS = "PRESENT";
    private static final String EVALUATED_STATUS = "EVALUATED";

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final TeacherScheduleService scheduleService;
    private final TeacherAnnouncementService announcementService;

    public TeacherDashboardService(
            UserRepository userRepository,
            SubjectRepository subjectRepository,
            EnrollmentRepository enrollmentRepository,
            AttendanceRepository attendanceRepository,
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            TeacherScheduleService scheduleService,
            TeacherAnnouncementService announcementService) {
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.scheduleService = scheduleService;
        this.announcementService = announcementService;
    }

    public TeacherDashboardResponse getDashboard(Long teacherId) {
        log.debug("Building dashboard for teacher id={}", teacherId);

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> ResourceNotFoundException.of("Teacher", teacherId));

        List<Subject> subjects = subjectRepository.findByTeacherIdWithRelations(teacherId);
        List<Long> subjectIds = subjects.stream().map(Subject::getId).toList();

        List<Enrollment> roster = enrollmentRepository.findBySubjectTeacherIdWithDetails(teacherId);
        long totalStudents = roster.stream().map(e -> e.getStudent().getId()).distinct().count();

        List<Assignment> assignments = assignmentRepository.findByTeacherId(teacherId);
        List<Attendance> attendanceRecords = attendanceRepository.findBySubjectTeacherId(teacherId);
        List<TeacherScheduleEntryResponse> todaysSchedule = scheduleService.getTodaysSchedule(teacherId);

        return TeacherDashboardResponse.builder()
                .teacherId(teacher.getId())
                .teacherName(teacher.getFirstName() + " " + (teacher.getLastName() != null ? teacher.getLastName() : ""))
                .totalSubjects(subjectIds.size())
                .totalStudents((int) totalStudents)
                .pendingReviewCount(countPendingReview(assignments))
                .attendancePendingToday(countAttendancePendingToday(subjects, attendanceRecords))
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

    private int countAttendancePendingToday(List<Subject> subjects, List<Attendance> attendanceRecords) {
        LocalDate today = LocalDate.now();
        var markedToday = attendanceRecords.stream()
                .filter(a -> a.getAttendanceDate().isEqual(today))
                .map(a -> a.getEnrollment().getSubject().getId())
                .collect(Collectors.toSet());
        return (int) subjects.stream().filter(s -> !markedToday.contains(s.getId())).count();
    }

    private List<SubjectAssignmentCountResponse> assignmentsPerSubject(List<Assignment> assignments) {
        Map<String, Long> counts = assignments.stream()
                .collect(Collectors.groupingBy(a -> a.getSubject().getSubjectName(), Collectors.counting()));
        return counts.entrySet().stream()
                .map(entry -> SubjectAssignmentCountResponse.builder()
                        .subjectName(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();
    }

    private List<AttendanceTrendPointResponse> attendanceTrend(List<Attendance> attendanceRecords) {
        Map<LocalDate, List<Attendance>> byDate = attendanceRecords.stream()
                .collect(Collectors.groupingBy(Attendance::getAttendanceDate));

        List<AttendanceTrendPointResponse> trend = new java.util.ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            List<Attendance> dayRecords = byDate.getOrDefault(date, List.of());
            int rate = 0;
            if (!dayRecords.isEmpty()) {
                long present = dayRecords.stream()
                        .filter(a -> PRESENT_STATUS.equalsIgnoreCase(a.getStatus()))
                        .count();
                rate = (int) Math.round((present * 100.0) / dayRecords.size());
            }
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
                        .subjectName(a.getSubject().getSubjectName())
                        .dueDate(a.getDueDate())
                        .maxMarks(a.getMaxMarks())
                        .build())
                .toList();
    }
}
