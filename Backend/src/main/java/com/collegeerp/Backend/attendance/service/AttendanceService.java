package com.collegeerp.Backend.attendance.service;

import com.collegeerp.Backend.attendance.dto.AttendanceRequest;
import com.collegeerp.Backend.attendance.dto.AttendanceResponse;
import com.collegeerp.Backend.attendance.entity.Attendance;
import com.collegeerp.Backend.attendance.repository.AttendanceRepository;
import com.collegeerp.Backend.enrollment.entity.Enrollment;
import com.collegeerp.Backend.enrollment.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            EnrollmentRepository enrollmentRepository) {

        this.attendanceRepository = attendanceRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public AttendanceResponse createAttendance(
            AttendanceRequest request) {

        if (attendanceRepository.existsByEnrollmentIdAndAttendanceDate(
                request.getEnrollmentId(),
                request.getAttendanceDate())) {

            throw new RuntimeException(
                    "Attendance already marked.");
        }

        Enrollment enrollment = enrollmentRepository.findById(
                        request.getEnrollmentId())
                .orElseThrow(() ->
                        new RuntimeException("Enrollment not found"));

        Attendance attendance = Attendance.builder()
                .enrollment(enrollment)
                .attendanceDate(request.getAttendanceDate())
                .status(request.getStatus())
                .remarks(request.getRemarks())
                .createdAt(LocalDateTime.now())
                .build();

        attendance = attendanceRepository.save(attendance);

        return map(attendance);
    }

    public List<AttendanceResponse> getAllAttendance() {

        return attendanceRepository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    public AttendanceResponse getAttendance(Long id) {

        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Attendance not found"));

        return map(attendance);
    }

    public void deleteAttendance(Long id) {

        if (!attendanceRepository.existsById(id)) {
            throw new RuntimeException("Attendance not found");
        }

        attendanceRepository.deleteById(id);
    }

    private AttendanceResponse map(Attendance attendance) {

        Enrollment e = attendance.getEnrollment();

        return AttendanceResponse.builder()
                .id(attendance.getId())
                .enrollmentId(e.getId())
                .studentId(e.getStudent().getId())
                .studentName(
                        e.getStudent().getFirstName()
                                + " "
                                + e.getStudent().getLastName())
                .subjectId(e.getSubject().getId())
                .subjectName(e.getSubject().getSubjectName())
                .attendanceDate(attendance.getAttendanceDate())
                .status(attendance.getStatus())
                .remarks(attendance.getRemarks())
                .build();
    }

}