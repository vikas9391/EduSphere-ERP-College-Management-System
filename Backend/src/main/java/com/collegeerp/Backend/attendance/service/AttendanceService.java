package com.collegeerp.Backend.attendance.service;

import com.collegeerp.Backend.attendance.dto.AttendanceRequest;
import com.collegeerp.Backend.attendance.dto.AttendanceResponse;
import com.collegeerp.Backend.attendance.dto.StudentAttendanceSummaryResponse;
import com.collegeerp.Backend.attendance.entity.Attendance;
import com.collegeerp.Backend.attendance.repository.AttendanceRepository;
import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.student.service.StudentIdentityService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class AttendanceService {
    private static final Set<String> VALID_STATUSES = Set.of("PRESENT", "ABSENT", "LATE", "EXCUSED");

    private final AttendanceRepository attendanceRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final StudentIdentityService studentIdentityService;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             ClassEnrollmentRepository classEnrollmentRepository,
                             StudentIdentityService studentIdentityService) {
        this.attendanceRepository = attendanceRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.studentIdentityService = studentIdentityService;
    }

    public AttendanceResponse createAttendance(AttendanceRequest request) {
        if (request.getClassEnrollmentId() == null) {
            throw new BadRequestException("Class enrollment is required");
        }
        if (request.getAttendanceDate() == null) {
            throw new BadRequestException("Attendance date is required");
        }

        ClassEnrollment classEnrollment = classEnrollmentRepository.findById(request.getClassEnrollmentId())
                .orElseThrow(() -> ResourceNotFoundException.of("Class enrollment", request.getClassEnrollmentId()));
        requireCanManageClassEnrollment(classEnrollment);

        if (attendanceRepository.existsByClassEnrollmentIdAndAttendanceDate(
                classEnrollment.getId(), request.getAttendanceDate())) {
            throw new DuplicateResourceException(
                    "Attendance has already been marked for this student and subject on " + request.getAttendanceDate());
        }

        Attendance attendance = Attendance.builder()
                .classEnrollment(classEnrollment)
                .attendanceDate(request.getAttendanceDate())
                .status(normalizeStatus(request.getStatus()))
                .remarks(normalizeRemarks(request.getRemarks()))
                .createdAt(LocalDateTime.now())
                .build();

        return map(attendanceRepository.save(attendance));
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAllAttendance() {
        UserPrincipal principal = currentPrincipal();
        if (isAdmin(principal)) {
            return attendanceRepository.findAll().stream().map(this::map).toList();
        }
        if ("TEACHER".equalsIgnoreCase(principal.getRole())) {
            return attendanceRepository.findClassAttendanceByTeacherId(principal.getId())
                    .stream().map(this::map).toList();
        }
        throw new AccessDeniedException("You are not allowed to view all attendance records");
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getStudentAttendance(Long studentId) {
        UserPrincipal principal = currentPrincipal();
        if (isAdmin(principal)) {
            return findStudentAttendanceRecords(studentId).stream().map(this::map).toList();
        }
        if (isStudentRole(principal)) {
            Long actualStudentId = studentIdentityService.requireStudentId(principal);
            if (!actualStudentId.equals(studentId)) {
                throw new AccessDeniedException("Students can only view their own attendance");
            }
            return findStudentAttendanceRecords(actualStudentId).stream().map(this::map).toList();
        }
        throw new AccessDeniedException("You are not allowed to view student attendance");
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMyAttendance() {
        UserPrincipal principal = currentPrincipal();
        if (!isStudentRole(principal)) {
            throw new AccessDeniedException("Only students can view their own attendance");
        }
        return findStudentAttendanceRecords(studentIdentityService.requireStudentId(principal))
                .stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public StudentAttendanceSummaryResponse getMyAttendanceSummary() {
        UserPrincipal principal = currentPrincipal();
        if (!isStudentRole(principal)) {
            throw new AccessDeniedException("Only students can view their own attendance");
        }

        Long studentId = studentIdentityService.requireStudentId(principal);
        List<ClassEnrollment> enrollments = classEnrollmentRepository.findAllByStudentId(studentId);
        List<Attendance> records = attendanceRepository.findClassAttendanceByStudentId(studentId);

        Map<String, StudentAttendanceSummaryResponse.SubjectAttendanceSummary> bySubject = new LinkedHashMap<>();
        for (ClassEnrollment enrollment : enrollments) {
            var classSubject = enrollment.getClassSubject();
            var subject = classSubject.getSubject();
            String key = subject != null ? "SUBJECT:" + subject.getId() : "CLASS_SUBJECT:" + classSubject.getId();
            bySubject.putIfAbsent(key, StudentAttendanceSummaryResponse.SubjectAttendanceSummary.builder()
                    .subjectId(subject != null ? subject.getId() : classSubject.getId())
                    .subjectCode(subject != null ? subject.getSubjectCode() : classSubject.getSubjectCode())
                    .subjectName(firstNonBlank(classSubject.getSubjectName(),
                            subject != null ? subject.getSubjectName() : null,
                            "Unknown Subject"))
                    .build());
        }

        int attended = 0;
        for (Attendance attendance : records) {
            var classSubject = attendance.getClassEnrollment().getClassSubject();
            var subject = classSubject.getSubject();
            String key = subject != null ? "SUBJECT:" + subject.getId() : "CLASS_SUBJECT:" + classSubject.getId();
            var summary = bySubject.computeIfAbsent(key, ignored ->
                    StudentAttendanceSummaryResponse.SubjectAttendanceSummary.builder()
                            .subjectId(subject != null ? subject.getId() : classSubject.getId())
                            .subjectCode(subject != null ? subject.getSubjectCode() : classSubject.getSubjectCode())
                            .subjectName(firstNonBlank(classSubject.getSubjectName(),
                                    subject != null ? subject.getSubjectName() : null,
                                    "Unknown Subject"))
                            .build());

            summary.setTotalClasses(summary.getTotalClasses() + 1);
            if (isPresent(attendance.getStatus())) {
                summary.setClassesAttended(summary.getClassesAttended() + 1);
                attended++;
            } else if (!"EXCUSED".equalsIgnoreCase(attendance.getStatus())) {
                summary.setClassesMissed(summary.getClassesMissed() + 1);
            }
        }

        bySubject.values().forEach(summary -> {
            int counted = summary.getClassesAttended() + summary.getClassesMissed();
            summary.setAttendancePercentage(counted == 0 ? 0 : round1(summary.getClassesAttended() * 100.0 / counted));
        });

        int countedOverall = records.stream()
                .map(Attendance::getStatus)
                .filter(status -> !"EXCUSED".equalsIgnoreCase(status))
                .mapToInt(ignored -> 1)
                .sum();

        return StudentAttendanceSummaryResponse.builder()
                .totalClasses(countedOverall)
                .classesAttended(attended)
                .classesMissed(countedOverall - attended)
                .overallAttendancePercentage(countedOverall == 0 ? 0 : round1(attended * 100.0 / countedOverall))
                .bySubject(new ArrayList<>(bySubject.values()))
                .build();
    }

    private List<Attendance> findStudentAttendanceRecords(Long studentId) {
        return attendanceRepository.findClassAttendanceByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public AttendanceResponse getAttendance(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Attendance", id));
        requireCanView(attendance.getClassEnrollment());
        return map(attendance);
    }

    public AttendanceResponse updateAttendance(Long id, AttendanceRequest request) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Attendance", id));
        ClassEnrollment classEnrollment = attendance.getClassEnrollment();
        requireCanManageClassEnrollment(classEnrollment);

        if (request.getClassEnrollmentId() == null
                || !request.getClassEnrollmentId().equals(classEnrollment.getId())) {
            throw new BadRequestException("Class enrollment cannot be changed when updating attendance");
        }
        if (request.getAttendanceDate() == null) {
            throw new BadRequestException("Attendance date is required");
        }

        if (attendanceRepository.existsByClassEnrollmentIdAndAttendanceDateAndIdNot(
                classEnrollment.getId(), request.getAttendanceDate(), id)) {
            throw new DuplicateResourceException(
                    "Attendance has already been marked for this student and subject on " + request.getAttendanceDate());
        }

        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(normalizeStatus(request.getStatus()));
        attendance.setRemarks(normalizeRemarks(request.getRemarks()));
        return map(attendanceRepository.save(attendance));
    }

    public void deleteAttendance(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Attendance", id));
        requireCanManageClassEnrollment(attendance.getClassEnrollment());
        attendanceRepository.delete(attendance);
    }

    private void requireCanManageClassEnrollment(ClassEnrollment enrollment) {
        if (enrollment == null || enrollment.getClassSubject() == null) {
            throw new BadRequestException("Attendance must belong to a valid class enrollment");
        }
        UserPrincipal principal = currentPrincipal();
        if (isAdmin(principal)) return;
        if (!"TEACHER".equalsIgnoreCase(principal.getRole())
                || enrollment.getClassSubject().getTeacher() == null
                || !principal.getId().equals(enrollment.getClassSubject().getTeacher().getId())) {
            throw new AccessDeniedException("Teachers can only manage attendance for their assigned class subjects");
        }
    }

    private void requireCanView(ClassEnrollment enrollment) {
        if (enrollment == null || enrollment.getClassSubject() == null) {
            throw new BadRequestException("Attendance must belong to a valid class enrollment");
        }
        UserPrincipal principal = currentPrincipal();
        if (isAdmin(principal)) return;
        if ("TEACHER".equalsIgnoreCase(principal.getRole())
                && enrollment.getClassSubject().getTeacher() != null
                && principal.getId().equals(enrollment.getClassSubject().getTeacher().getId())) return;
        if (isStudentRole(principal)
                && studentIdentityService.requireStudentId(principal).equals(enrollment.getStudent().getId())) return;
        throw new AccessDeniedException("You are not allowed to view this attendance record");
    }

    private boolean isStudentRole(UserPrincipal principal) {
        return "STUDENT".equalsIgnoreCase(principal.getRole());
    }

    private boolean isAdmin(UserPrincipal principal) {
        return "ADMIN".equalsIgnoreCase(principal.getRole())
                || "SUPER_ADMIN".equalsIgnoreCase(principal.getRole());
    }

    private UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("Authentication is required");
        }
        return (UserPrincipal) authentication.getPrincipal();
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Attendance status is required");
        }
        String status = value.trim().toUpperCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(status)) {
            throw new BadRequestException("Invalid attendance status. Allowed values: " + VALID_STATUSES);
        }
        return status;
    }

    private String normalizeRemarks(String value) {
        if (value == null) return null;
        String remarks = value.trim();
        return remarks.isEmpty() ? null : remarks;
    }

    private boolean isPresent(String status) {
        return "PRESENT".equalsIgnoreCase(status) || "LATE".equalsIgnoreCase(status);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "Unknown Subject";
    }

    private AttendanceResponse map(Attendance attendance) {
        ClassEnrollment enrollment = attendance.getClassEnrollment();
        var classSubject = enrollment.getClassSubject();
        var subject = classSubject.getSubject();

        return AttendanceResponse.builder()
                .id(attendance.getId())
                .classEnrollmentId(enrollment.getId())
                .studentId(enrollment.getStudent().getId())
                .studentName((enrollment.getStudent().getFirstName() + " "
                        + (enrollment.getStudent().getLastName() == null ? "" : enrollment.getStudent().getLastName())).trim())
                .subjectId(subject != null ? subject.getId() : classSubject.getId())
                .subjectName(firstNonBlank(classSubject.getSubjectName(),
                        subject != null ? subject.getSubjectName() : null,
                        "Unknown Subject"))
                .attendanceDate(attendance.getAttendanceDate())
                .status(attendance.getStatus())
                .remarks(attendance.getRemarks())
                .build();
    }
}
