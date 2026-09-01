package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.attendance.entity.Attendance;
import com.collegeerp.Backend.attendance.repository.AttendanceRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.student.dto.StudentAttendanceResponse;
import com.collegeerp.Backend.student.dto.SubjectAttendanceResponse;
import com.collegeerp.Backend.subject.entity.Subject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Self-service attendance view for the logged-in student.
 *
 * ClassEnrollment is the authoritative operational student-subject relationship. Legacy
 * Enrollment-backed attendance is still included for compatibility while old data is being
 * reconciled, but every class-based attendance record is resolved through:
 * Attendance -> ClassEnrollment -> ClassSubject -> Subject.
 */
@Service
@Transactional(readOnly = true)
public class StudentAttendanceService {

    private static final String PRESENT_STATUS = "PRESENT";

    private final AttendanceRepository attendanceRepository;

    public StudentAttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    public StudentAttendanceResponse getAttendance(Long studentId) {
        // Class-based attendance is the canonical source. Legacy rows are retained only for
        // compatibility with data that has not yet been migrated to ClassEnrollment.
        List<Attendance> classRecords = attendanceRepository.findClassAttendanceByStudentId(studentId);
        List<Attendance> legacyRecords = attendanceRepository.findLegacyAttendanceByStudentId(studentId);

        List<Attendance> records = new ArrayList<>(classRecords.size() + legacyRecords.size());
        records.addAll(classRecords);
        records.addAll(legacyRecords);

        long total = records.size();
        long attended = records.stream().filter(this::isAttended).count();
        long missed = total - attended;

        // Start with every current class-enrollment subject, including subjects with zero
        // attendance records, then add legacy-only subjects for compatibility.
        Map<Long, SubjectAttendanceAccumulator> bySubject = new HashMap<>();

        for (ClassEnrollment classEnrollment : getCurrentClassEnrollments(classRecords)) {
            Subject subject = classEnrollment.getClassSubject().getSubject();
            if (subject != null) {
                bySubject.computeIfAbsent(subject.getId(), ignored -> new SubjectAttendanceAccumulator(subject));
            }
        }

        for (Attendance attendance : records) {
            Subject subject = resolveSubject(attendance);
            if (subject == null) {
                continue;
            }
            SubjectAttendanceAccumulator accumulator =
                    bySubject.computeIfAbsent(subject.getId(), ignored -> new SubjectAttendanceAccumulator(subject));
            accumulator.total++;
            if (isAttended(attendance)) {
                accumulator.attended++;
            }
        }

        List<SubjectAttendanceResponse> subjectBreakdown = bySubject.values().stream()
                .map(SubjectAttendanceAccumulator::toResponse)
                .sorted(Comparator.comparing(
                        SubjectAttendanceResponse::getSubjectCode,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        return StudentAttendanceResponse.builder()
                .totalClasses(total)
                .classesAttended(attended)
                .classesMissed(missed)
                .overallAttendancePercentage(percentage(attended, total))
                .bySubject(subjectBreakdown)
                .build();
    }

    private List<ClassEnrollment> getCurrentClassEnrollments(List<Attendance> classRecords) {
        return classRecords.stream()
                .map(Attendance::getClassEnrollment)
                .filter(java.util.Objects::nonNull)
                .filter(ce -> ce.getClassSubject() != null)
                .distinct()
                .toList();
    }

    private Subject resolveSubject(Attendance attendance) {
        if (attendance.getClassEnrollment() != null
                && attendance.getClassEnrollment().getClassSubject() != null) {
            return attendance.getClassEnrollment().getClassSubject().getSubject();
        }
        if (attendance.getEnrollment() != null) {
            return attendance.getEnrollment().getSubject();
        }
        return null;
    }

    private boolean isAttended(Attendance attendance) {
        return PRESENT_STATUS.equalsIgnoreCase(attendance.getStatus());
    }

    private double percentage(long attended, long total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round((attended * 10000.0) / total) / 100.0;
    }

    private static final class SubjectAttendanceAccumulator {
        private final Subject subject;
        private long total;
        private long attended;

        private SubjectAttendanceAccumulator(Subject subject) {
            this.subject = subject;
        }

        private SubjectAttendanceResponse toResponse() {
            return SubjectAttendanceResponse.builder()
                    .subjectId(subject.getId())
                    .subjectCode(subject.getSubjectCode())
                    .subjectName(subject.getSubjectName())
                    .totalClasses(total)
                    .classesAttended(attended)
                    .classesMissed(total - attended)
                    .attendancePercentage(percentage(attended, total))
                    .build();
        }

        private static double percentage(long attended, long total) {
            if (total == 0) {
                return 0.0;
            }
            return Math.round((attended * 10000.0) / total) / 100.0;
        }
    }
}
