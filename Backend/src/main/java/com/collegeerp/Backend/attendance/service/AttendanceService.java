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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class AttendanceService {

    private static final Set<String> VALID_STATUSES = Set.of(
            "PRESENT", "ABSENT", "LATE", "EXCUSED"
    );

    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            EnrollmentRepository enrollmentRepository) {

        this.attendanceRepository = attendanceRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public AttendanceResponse createAttendance(AttendanceRequest request) {

        Enrollment enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> ResourceNotFoundException.of("Enrollment", request.getEnrollmentId()));

        if (attendanceRepository.existsByEnrollmentIdAndAttendanceDate(
                enrollment.getId(), request.getAttendanceDate())) {
            throw new DuplicateResourceException(
                    "Attendance has already been marked for this student and subject on "
                            + request.getAttendanceDate());
        }

        String status = normalizeStatus(request.getStatus());

        Attendance attendance = Attendance.builder()
                .enrollment(enrollment)
                .attendanceDate(request.getAttendanceDate())
                .status(status)
                .remarks(normalizeRemarks(request.getRemarks()))
                .createdAt(LocalDateTime.now())
                .build();

        return map(attendanceRepository.save(attendance));
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAllAttendance() {
        return attendanceRepository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public AttendanceResponse getAttendance(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Attendance", id));
        return map(attendance);
    }

    public void deleteAttendance(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Attendance", id));
        attendanceRepository.delete(attendance);
    }

    private String normalizeStatus(String value) {
        String status = value.trim().toUpperCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(status)) {
            throw new BadRequestException(
                    "Invalid attendance status. Allowed values: " + VALID_STATUSES);
        }
        return status;
    }

    private String normalizeRemarks(String value) {
        if (value == null) {
            return null;
        }
        String remarks = value.trim();
        return remarks.isEmpty() ? null : remarks;
    }

    private AttendanceResponse map(Attendance attendance) {
        Enrollment enrollment = attendance.getEnrollment();

        return AttendanceResponse.builder()
                .id(attendance.getId())
                .enrollmentId(enrollment.getId())
                .studentId(enrollment.getStudent().getId())
                .studentName(enrollment.getStudent().getFirstName()
                        + " " + enrollment.getStudent().getLastName())
                .subjectId(enrollment.getSubject().getId())
                .subjectName(enrollment.getSubject().getSubjectName())
                .attendanceDate(attendance.getAttendanceDate())
                .status(attendance.getStatus())
                .remarks(attendance.getRemarks())
                .build();
    }

}