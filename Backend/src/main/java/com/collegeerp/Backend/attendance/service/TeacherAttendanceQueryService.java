package com.collegeerp.Backend.attendance.service;

import com.collegeerp.Backend.attendance.dto.AttendanceResponse;
import com.collegeerp.Backend.attendance.entity.Attendance;
import com.collegeerp.Backend.attendance.repository.AttendanceRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Database-scoped teacher attendance reader; avoids loading the tenant's full attendance table. */
@Service
@Transactional(readOnly = true)
public class TeacherAttendanceQueryService {

    private final AttendanceRepository attendanceRepository;

    public TeacherAttendanceQueryService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    public List<AttendanceResponse> getTeacherAttendance(Long teacherId) {
        List<Attendance> classRows = attendanceRepository.findClassAttendanceByTeacherId(teacherId);
        List<Attendance> legacyRows = attendanceRepository.findBySubjectTeacherId(teacherId);

        Set<String> classKeys = new HashSet<>();
        classRows.forEach(a -> classKeys.add(key(a)));

        List<Attendance> merged = new ArrayList<>(classRows);
        for (Attendance legacy : legacyRows) {
            if (!classKeys.contains(key(legacy))) {
                merged.add(legacy);
            }
        }

        return merged.stream().map(this::map).toList();
    }

    private String key(Attendance attendance) {
        Long studentId;
        String subjectKey;
        ClassEnrollment ce = attendance.getClassEnrollment();
        if (ce != null && ce.getClassSubject() != null) {
            studentId = ce.getStudent().getId();
            subjectKey = ce.getClassSubject().getSubject() != null
                    ? "SUBJECT:" + ce.getClassSubject().getSubject().getId()
                    : "CLASS_SUBJECT:" + ce.getClassSubject().getId();
        } else {
            studentId = attendance.getEnrollment().getStudent().getId();
            subjectKey = "SUBJECT:" + attendance.getEnrollment().getSubject().getId();
        }
        return studentId + "|" + subjectKey + "|" + attendance.getAttendanceDate();
    }

    private AttendanceResponse map(Attendance attendance) {
        ClassEnrollment ce = attendance.getClassEnrollment();
        var enrollment = attendance.getEnrollment();

        Long studentId = ce != null ? ce.getStudent().getId() : enrollment.getStudent().getId();
        String studentName = ce != null
                ? ce.getStudent().getFirstName() + " " +
                  (ce.getStudent().getLastName() != null ? ce.getStudent().getLastName() : "")
                : enrollment.getStudent().getFirstName() + " " +
                  (enrollment.getStudent().getLastName() != null ? enrollment.getStudent().getLastName() : "");

        Long subjectId;
        String subjectName;
        if (ce != null && ce.getClassSubject() != null) {
            var cs = ce.getClassSubject();
            subjectId = cs.getSubject() != null ? cs.getSubject().getId() : cs.getId();
            subjectName = cs.getSubjectName();
        } else {
            subjectId = enrollment.getSubject().getId();
            subjectName = enrollment.getSubject().getSubjectName();
        }

        return AttendanceResponse.builder()
                .id(attendance.getId())
                .enrollmentId(enrollment != null ? enrollment.getId() : null)
                .classEnrollmentId(ce != null ? ce.getId() : null)
                .studentId(studentId)
                .studentName(studentName.trim())
                .subjectId(subjectId)
                .subjectName(subjectName)
                .attendanceDate(attendance.getAttendanceDate())
                .status(attendance.getStatus())
                .remarks(attendance.getRemarks())
                .build();
    }
}
