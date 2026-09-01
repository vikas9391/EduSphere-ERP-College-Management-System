package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.attendance.entity.Attendance;
import com.collegeerp.Backend.attendance.repository.AttendanceRepository;
import com.collegeerp.Backend.attendance.service.AttendanceStatusPolicy;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.student.dto.StudentAttendanceResponse;
import com.collegeerp.Backend.student.dto.SubjectAttendanceResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Self-service attendance view for the logged-in student. ClassEnrollment is authoritative;
 * legacy Enrollment attendance is retained only when no migrated class record exists for the
 * same student/subject/date.
 */
@Service
@Transactional(readOnly = true)
public class StudentAttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    public StudentAttendanceService(
            AttendanceRepository attendanceRepository,
            ClassEnrollmentRepository classEnrollmentRepository) {
        this.attendanceRepository = attendanceRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
    }

    public StudentAttendanceResponse getAttendance(Long studentId) {
        List<ClassEnrollment> currentEnrollments = classEnrollmentRepository.findAllByStudentId(studentId);
        List<Attendance> records = mergedAttendance(studentId);

        long total = records.stream()
                .filter(a -> AttendanceStatusPolicy.countsTowardPercentage(a.getStatus()))
                .count();
        long attended = records.stream()
                .filter(a -> AttendanceStatusPolicy.countsTowardPercentage(a.getStatus()))
                .filter(a -> AttendanceStatusPolicy.isAttended(a.getStatus()))
                .count();
        long missed = total - attended;

        Map<String, SubjectAttendanceAccumulator> bySubject = new LinkedHashMap<>();

        // Seed every current ClassEnrollment so zero-attendance subjects still appear.
        for (ClassEnrollment enrollment : currentEnrollments) {
            if (enrollment.getClassSubject() == null) {
                continue;
            }
            var cs = enrollment.getClassSubject();
            String key = subjectKey(cs.getId(), cs.getSubject() != null ? cs.getSubject().getId() : null);
            bySubject.putIfAbsent(key, new SubjectAttendanceAccumulator(
                    cs.getSubject() != null ? cs.getSubject().getId() : cs.getId(),
                    cs.getSubject() != null ? cs.getSubject().getSubjectCode() : cs.getSubjectCode(),
                    cs.getSubjectName()));
        }

        // Add attendance counts, including legacy-only historical subjects during migration.
        for (Attendance attendance : records) {
            ResolvedSubject subject = resolveSubject(attendance);
            if (subject == null) {
                continue;
            }
            SubjectAttendanceAccumulator accumulator = bySubject.computeIfAbsent(
                    subject.key(),
                    ignored -> new SubjectAttendanceAccumulator(subject.id(), subject.code(), subject.name()));

            if (!AttendanceStatusPolicy.countsTowardPercentage(attendance.getStatus())) {
                continue;
            }
            accumulator.total++;
            if (AttendanceStatusPolicy.isAttended(attendance.getStatus())) {
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

    private List<Attendance> mergedAttendance(Long studentId) {
        List<Attendance> classRecords = attendanceRepository.findClassAttendanceByStudentId(studentId);
        List<Attendance> legacyRecords = attendanceRepository.findLegacyAttendanceByStudentId(studentId);

        Set<String> authoritativeKeys = classRecords.stream()
                .map(this::studentSubjectDateKey)
                .collect(Collectors.toSet());

        List<Attendance> merged = new ArrayList<>(classRecords);
        legacyRecords.stream()
                .filter(a -> !authoritativeKeys.contains(studentSubjectDateKey(a)))
                .forEach(merged::add);
        return merged;
    }

    private String studentSubjectDateKey(Attendance attendance) {
        ResolvedSubject subject = resolveSubject(attendance);
        String subjectKey = subject != null ? subject.key() : "ATTENDANCE:" + attendance.getId();
        return subjectKey + "|" + attendance.getAttendanceDate();
    }

    private ResolvedSubject resolveSubject(Attendance attendance) {
        if (attendance.getClassEnrollment() != null
                && attendance.getClassEnrollment().getClassSubject() != null) {
            var cs = attendance.getClassEnrollment().getClassSubject();
            if (cs.getSubject() != null) {
                return new ResolvedSubject(
                        subjectKey(cs.getId(), cs.getSubject().getId()),
                        cs.getSubject().getId(),
                        cs.getSubject().getSubjectCode(),
                        cs.getSubjectName());
            }
            return new ResolvedSubject(
                    subjectKey(cs.getId(), null),
                    cs.getId(),
                    cs.getSubjectCode(),
                    cs.getSubjectName());
        }
        if (attendance.getEnrollment() != null && attendance.getEnrollment().getSubject() != null) {
            var subject = attendance.getEnrollment().getSubject();
            return new ResolvedSubject(
                    "SUBJECT:" + subject.getId(),
                    subject.getId(),
                    subject.getSubjectCode(),
                    subject.getSubjectName());
        }
        return null;
    }

    private String subjectKey(Long classSubjectId, Long formalSubjectId) {
        return formalSubjectId != null
                ? "SUBJECT:" + formalSubjectId
                : "CLASS_SUBJECT:" + classSubjectId;
    }

    private static double percentage(long attended, long total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round((attended * 10000.0) / total) / 100.0;
    }

    private record ResolvedSubject(String key, Long id, String code, String name) {}

    private static final class SubjectAttendanceAccumulator {
        private final Long id;
        private final String code;
        private final String name;
        private long total;
        private long attended;

        private SubjectAttendanceAccumulator(Long id, String code, String name) {
            this.id = id;
            this.code = code;
            this.name = name;
        }

        private SubjectAttendanceResponse toResponse() {
            return SubjectAttendanceResponse.builder()
                    .subjectId(id)
                    .subjectCode(code)
                    .subjectName(name)
                    .totalClasses(total)
                    .classesAttended(attended)
                    .classesMissed(total - attended)
                    .attendancePercentage(percentage(attended, total))
                    .build();
        }
    }
}
