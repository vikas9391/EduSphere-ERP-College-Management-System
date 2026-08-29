package com.collegeerp.Backend.attendance.service;

import com.collegeerp.Backend.attendance.dto.AttendanceRequest;
import com.collegeerp.Backend.attendance.dto.AttendanceResponse;
import com.collegeerp.Backend.attendance.entity.Attendance;
import com.collegeerp.Backend.attendance.repository.AttendanceRepository;
import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.enrollment.entity.Enrollment;
import com.collegeerp.Backend.enrollment.repository.EnrollmentRepository;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class AttendanceService {
    private static final Set<String> VALID_STATUSES = Set.of("PRESENT", "ABSENT", "LATE", "EXCUSED");
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    public AttendanceService(AttendanceRepository attendanceRepository, EnrollmentRepository enrollmentRepository,
                             StudentRepository studentRepository, ClassEnrollmentRepository classEnrollmentRepository) {
        this.attendanceRepository = attendanceRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
    }

    public AttendanceResponse createAttendance(AttendanceRequest request) {
        if (request.getAttendanceDate() == null) throw new BadRequestException("Attendance date is required");
        if (request.getClassEnrollmentId() != null) {
            ClassEnrollment classEnrollment = classEnrollmentRepository.findById(request.getClassEnrollmentId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Class enrollment", request.getClassEnrollmentId()));
            requireCanManageClassEnrollment(classEnrollment);
            if (attendanceRepository.existsByClassEnrollmentIdAndAttendanceDate(classEnrollment.getId(), request.getAttendanceDate())) {
                throw new DuplicateResourceException("Attendance has already been marked for this student and subject on " + request.getAttendanceDate());
            }
            Attendance attendance = Attendance.builder()
                    .classEnrollment(classEnrollment).attendanceDate(request.getAttendanceDate())
                    .status(normalizeStatus(request.getStatus())).remarks(normalizeRemarks(request.getRemarks()))
                    .createdAt(LocalDateTime.now()).build();
            return map(attendanceRepository.save(attendance));
        }
        if (request.getEnrollmentId() == null) throw new BadRequestException("Enrollment or class enrollment is required");
        Enrollment enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> ResourceNotFoundException.of("Enrollment", request.getEnrollmentId()));
        requireCanManageSubject(enrollment);
        if (attendanceRepository.existsByEnrollmentIdAndAttendanceDate(enrollment.getId(), request.getAttendanceDate())) {
            throw new DuplicateResourceException("Attendance has already been marked for this student and subject on " + request.getAttendanceDate());
        }
        Attendance attendance = Attendance.builder()
                .enrollment(enrollment).attendanceDate(request.getAttendanceDate())
                .status(normalizeStatus(request.getStatus())).remarks(normalizeRemarks(request.getRemarks()))
                .createdAt(LocalDateTime.now()).build();
        return map(attendanceRepository.save(attendance));
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAllAttendance() {
        UserPrincipal principal = currentPrincipal();
        if (isAdmin(principal)) return attendanceRepository.findAll().stream().map(this::map).toList();
        if ("TEACHER".equalsIgnoreCase(principal.getRole())) {
            return attendanceRepository.findAll().stream().filter(a ->
                    (a.getClassEnrollment() != null && a.getClassEnrollment().getClassSubject().getTeacher() != null
                            && principal.getId().equals(a.getClassEnrollment().getClassSubject().getTeacher().getId()))
                    || (a.getEnrollment() != null && a.getEnrollment().getSubject().getTeacher() != null
                            && principal.getId().equals(a.getEnrollment().getSubject().getTeacher().getId())))
                    .map(this::map).toList();
        }
        throw new AccessDeniedException("You are not allowed to view all attendance records");
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getStudentAttendance(Long studentId) {
        UserPrincipal principal = currentPrincipal();
        if (isAdmin(principal)) return attendanceRepository.findAll().stream().filter(a ->
                    (a.getClassEnrollment() != null && a.getClassEnrollment().getStudent().getId().equals(studentId))
                    || (a.getEnrollment() != null && a.getEnrollment().getStudent().getId().equals(studentId)))
                    .map(this::map).toList();
        if (isStudentRole(principal)) {
            Long actualStudentId = resolveStudentId(principal);
            if (!actualStudentId.equals(studentId)) throw new AccessDeniedException("Students can only view their own attendance");
            return attendanceRepository.findAll().stream().filter(a ->
                    (a.getClassEnrollment() != null && a.getClassEnrollment().getStudent().getId().equals(actualStudentId))
                    || (a.getEnrollment() != null && a.getEnrollment().getStudent().getId().equals(actualStudentId)))
                    .map(this::map).toList();
        }
        throw new AccessDeniedException("You are not allowed to view student attendance");
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMyAttendance() {
        UserPrincipal principal = currentPrincipal();
        if (!isStudentRole(principal)) throw new AccessDeniedException("Only students can view their own attendance");
        Long studentId = resolveStudentId(principal);
        return attendanceRepository.findAll().stream().filter(a ->
                    (a.getClassEnrollment() != null && a.getClassEnrollment().getStudent().getId().equals(studentId))
                    || (a.getEnrollment() != null && a.getEnrollment().getStudent().getId().equals(studentId)))
                    .map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public AttendanceResponse getAttendance(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Attendance", id));
        if (attendance.getClassEnrollment() != null) requireCanView(attendance.getClassEnrollment());
        else requireCanView(attendance.getEnrollment());
        return map(attendance);
    }


    public AttendanceResponse updateAttendance(Long id, AttendanceRequest request) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Attendance", id));
        if (attendance.getClassEnrollment() != null) requireCanManageClassEnrollment(attendance.getClassEnrollment());
        else requireCanManageSubject(attendance.getEnrollment());
        if (request.getAttendanceDate() == null) throw new BadRequestException("Attendance date is required");
        String status = normalizeStatus(request.getStatus());
        if (attendance.getClassEnrollment() != null
                ? attendanceRepository.existsByClassEnrollmentIdAndAttendanceDateAndIdNot(attendance.getClassEnrollment().getId(), request.getAttendanceDate(), id)
                : attendanceRepository.existsByEnrollmentIdAndAttendanceDateAndIdNot(attendance.getEnrollment().getId(), request.getAttendanceDate(), id)) {
            throw new DuplicateResourceException("Attendance has already been marked for this student and subject on " + request.getAttendanceDate());
        }
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(status);
        attendance.setRemarks(normalizeRemarks(request.getRemarks()));
        return map(attendanceRepository.save(attendance));
    }

    public void deleteAttendance(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Attendance", id));
        if (attendance.getClassEnrollment() != null) requireCanManageClassEnrollment(attendance.getClassEnrollment());
        else requireCanManageSubject(attendance.getEnrollment());
        attendanceRepository.delete(attendance);
    }

    private void requireCanManageSubject(Enrollment enrollment) {
        UserPrincipal principal = currentPrincipal();
        if (isAdmin(principal)) return;
        if (!"TEACHER".equalsIgnoreCase(principal.getRole())
                || enrollment.getSubject().getTeacher() == null
                || !principal.getId().equals(enrollment.getSubject().getTeacher().getId())) {
            throw new AccessDeniedException("Teachers can only manage attendance for their assigned subjects");
        }
    }

    private void requireCanManageClassEnrollment(ClassEnrollment enrollment) {
        UserPrincipal principal = currentPrincipal();
        if (isAdmin(principal)) return;
        if (!"TEACHER".equalsIgnoreCase(principal.getRole())
                || enrollment.getClassSubject().getTeacher() == null
                || !principal.getId().equals(enrollment.getClassSubject().getTeacher().getId())) {
            throw new AccessDeniedException("Teachers can only manage attendance for their assigned class subjects");
        }
    }

    private void requireCanView(ClassEnrollment enrollment) {
        UserPrincipal principal = currentPrincipal();
        if (isAdmin(principal)) return;
        if ("TEACHER".equalsIgnoreCase(principal.getRole())
                && enrollment.getClassSubject().getTeacher() != null
                && principal.getId().equals(enrollment.getClassSubject().getTeacher().getId())) return;
        if (isStudentRole(principal) && resolveStudentId(principal).equals(enrollment.getStudent().getId())) return;
        throw new AccessDeniedException("You are not allowed to view this attendance record");
    }

    private void requireCanView(Enrollment enrollment) {
        UserPrincipal principal = currentPrincipal();
        if (isAdmin(principal)) return;
        if ("TEACHER".equalsIgnoreCase(principal.getRole())
                && enrollment.getSubject().getTeacher() != null
                && principal.getId().equals(enrollment.getSubject().getTeacher().getId())) return;
        if (isStudentRole(principal) && resolveStudentId(principal).equals(enrollment.getStudent().getId())) return;
        throw new AccessDeniedException("You are not allowed to view this attendance record");
    }

    private Long resolveStudentId(UserPrincipal principal) {
        // Student accounts are authenticated through the common JWT user principal.
        // Resolve the domain Student by email first; older/demo tokens may carry the
        // Student id directly, so keep the principal id as a safe compatibility fallback.
        if (principal.getEmail() != null) {
            Student student = studentRepository.findByEmail(principal.getEmail()).orElse(null);
            if (student != null) return student.getId();
        }
        if (principal.getId() != null && studentRepository.existsById(principal.getId())) {
            return principal.getId();
        }
        throw new ResourceNotFoundException("Student profile not found for authenticated user");
    }

    private boolean isStudentRole(UserPrincipal principal) {
        String role = principal.getRole() == null ? "" : principal.getRole().trim();
        return "STUDENT".equalsIgnoreCase(role) || "STUDENTS".equalsIgnoreCase(role);
    }

    private boolean isAdmin(UserPrincipal principal) {
        return "ADMIN".equalsIgnoreCase(principal.getRole()) || "SUPER_ADMIN".equalsIgnoreCase(principal.getRole());
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
        if (value == null || value.isBlank()) throw new BadRequestException("Attendance status is required");
        String status = value.trim().toUpperCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(status)) throw new BadRequestException("Invalid attendance status. Allowed values: " + VALID_STATUSES);
        return status;
    }

    private String normalizeRemarks(String value) {
        if (value == null) return null;
        String remarks = value.trim();
        return remarks.isEmpty() ? null : remarks;
    }

    private AttendanceResponse map(Attendance attendance) {
        Enrollment enrollment = attendance.getEnrollment();
        ClassEnrollment ce = attendance.getClassEnrollment();
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .enrollmentId(enrollment != null ? enrollment.getId() : null)
                .classEnrollmentId(ce != null ? ce.getId() : null)
                .studentId(ce != null ? ce.getStudent().getId() : enrollment.getStudent().getId())
                .studentName(ce != null ? ce.getStudent().getFirstName() + " " + ce.getStudent().getLastName()
                        : enrollment.getStudent().getFirstName() + " " + enrollment.getStudent().getLastName())
                .subjectId(ce != null && ce.getClassSubject().getSubject() != null
                        ? ce.getClassSubject().getSubject().getId() : (enrollment != null ? enrollment.getSubject().getId() : null))
                .subjectName(ce != null ? ce.getClassSubject().getSubjectName() : enrollment.getSubject().getSubjectName())
                .attendanceDate(attendance.getAttendanceDate()).status(attendance.getStatus()).remarks(attendance.getRemarks())
                .build();
    }
}