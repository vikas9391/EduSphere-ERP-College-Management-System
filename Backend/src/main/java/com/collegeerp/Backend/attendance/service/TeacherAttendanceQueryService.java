package com.collegeerp.Backend.attendance.service;

import com.collegeerp.Backend.attendance.dto.AttendanceResponse;
import com.collegeerp.Backend.attendance.entity.Attendance;
import com.collegeerp.Backend.attendance.repository.AttendanceRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Database-scoped teacher attendance reader for class-scoped attendance records. */
@Service
@Transactional(readOnly = true)
public class TeacherAttendanceQueryService {

    private final AttendanceRepository attendanceRepository;

    public TeacherAttendanceQueryService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    public List<AttendanceResponse> getTeacherAttendance(Long teacherId) {
        return attendanceRepository.findClassAttendanceByTeacherId(teacherId)
                .stream()
                .map(this::map)
                .toList();
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
                .subjectName(classSubject.getSubjectName() != null
                        ? classSubject.getSubjectName()
                        : subject != null ? subject.getSubjectName() : "Unknown Subject")
                .attendanceDate(attendance.getAttendanceDate())
                .status(attendance.getStatus())
                .remarks(attendance.getRemarks())
                .build();
    }
}
