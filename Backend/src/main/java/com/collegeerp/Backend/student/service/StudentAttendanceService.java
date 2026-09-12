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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Student self-service attendance backed exclusively by ClassEnrollment. */
@Service
@Transactional(readOnly = true)
public class StudentAttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    public StudentAttendanceService(AttendanceRepository attendanceRepository,
                                    ClassEnrollmentRepository classEnrollmentRepository) {
        this.attendanceRepository = attendanceRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
    }

    public StudentAttendanceResponse getAttendance(Long studentId) {
        List<ClassEnrollment> currentEnrollments = classEnrollmentRepository.findAllByStudentId(studentId);
        List<Attendance> records = attendanceRepository.findClassAttendanceByStudentId(studentId);

        long total = records.stream()
                .filter(a -> AttendanceStatusPolicy.countsTowardPercentage(a.getStatus()))
                .count();
        long attended = records.stream()
                .filter(a -> AttendanceStatusPolicy.countsTowardPercentage(a.getStatus()))
                .filter(a -> AttendanceStatusPolicy.isAttended(a.getStatus()))
                .count();

        Map<String, SubjectAttendanceAccumulator> bySubject = new LinkedHashMap<>();
        for (ClassEnrollment enrollment : currentEnrollments) {
            var cs = enrollment.getClassSubject();
            if (cs == null) continue;
            var subject = cs.getSubject();
            String key = subject != null ? "SUBJECT:" + subject.getId() : "CLASS_SUBJECT:" + cs.getId();
            bySubject.putIfAbsent(key, new SubjectAttendanceAccumulator(
                    subject != null ? subject.getId() : cs.getId(),
                    subject != null ? subject.getSubjectCode() : cs.getSubjectCode(),
                    cs.getSubjectName()));
        }

        for (Attendance attendance : records) {
            var cs = attendance.getClassEnrollment().getClassSubject();
            var subject = cs.getSubject();
            String key = subject != null ? "SUBJECT:" + subject.getId() : "CLASS_SUBJECT:" + cs.getId();
            SubjectAttendanceAccumulator accumulator = bySubject.computeIfAbsent(key,
                    ignored -> new SubjectAttendanceAccumulator(
                            subject != null ? subject.getId() : cs.getId(),
                            subject != null ? subject.getSubjectCode() : cs.getSubjectCode(),
                            cs.getSubjectName()));

            if (AttendanceStatusPolicy.countsTowardPercentage(attendance.getStatus())) {
                accumulator.total++;
                if (AttendanceStatusPolicy.isAttended(attendance.getStatus())) {
                    accumulator.attended++;
                }
            }
        }

        List<SubjectAttendanceResponse> subjectBreakdown = bySubject.values().stream()
                .map(SubjectAttendanceAccumulator::toResponse)
                .sorted(Comparator.comparing(SubjectAttendanceResponse::getSubjectCode,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        return StudentAttendanceResponse.builder()
                .totalClasses(total)
                .classesAttended(attended)
                .classesMissed(total - attended)
                .overallAttendancePercentage(percentage(attended, total))
                .bySubject(subjectBreakdown)
                .build();
    }

    private static double percentage(long attended, long total) {
        return total == 0 ? 0.0 : Math.round((attended * 10000.0) / total) / 100.0;
    }

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
