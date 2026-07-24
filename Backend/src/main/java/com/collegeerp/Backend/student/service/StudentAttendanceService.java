package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.attendance.entity.Attendance;
import com.collegeerp.Backend.attendance.repository.AttendanceRepository;
import com.collegeerp.Backend.student.dto.StudentAttendanceResponse;
import com.collegeerp.Backend.student.dto.SubjectAttendanceResponse;
import com.collegeerp.Backend.subject.entity.Subject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Self-service attendance view for the logged-in student. A class is counted as "attended"
 * when {@link Attendance#getStatus()} equals {@code "PRESENT"} (case-insensitive); every other
 * status value (e.g. {@code "ABSENT"}, {@code "LATE"}) counts as missed. {@code status} is a
 * free-text column on the entity rather than an enum, so this is a deliberate, documented
 * convention rather than an exhaustive status mapping - if additional statuses are introduced
 * that should count as "attended" (e.g. an excused-late arrival), update {@link #isAttended}.
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

        List<Attendance> records = attendanceRepository.findByStudentId(studentId);

        long total = records.size();
        long attended = records.stream().filter(this::isAttended).count();
        long missed = total - attended;

        Map<Subject, List<Attendance>> bySubject = records.stream()
                .collect(Collectors.groupingBy(a -> a.getEnrollment().getSubject()));

        List<SubjectAttendanceResponse> subjectBreakdown = bySubject.entrySet().stream()
                .map(entry -> toSubjectAttendance(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(SubjectAttendanceResponse::getSubjectCode))
                .toList();

        return StudentAttendanceResponse.builder()
                .totalClasses(total)
                .classesAttended(attended)
                .classesMissed(missed)
                .overallAttendancePercentage(percentage(attended, total))
                .bySubject(subjectBreakdown)
                .build();
    }

    private SubjectAttendanceResponse toSubjectAttendance(Subject subject, List<Attendance> records) {
        long total = records.size();
        long attended = records.stream().filter(this::isAttended).count();

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

    private boolean isAttended(Attendance attendance) {
        return PRESENT_STATUS.equalsIgnoreCase(attendance.getStatus());
    }

    private double percentage(long attended, long total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round((attended * 10000.0) / total) / 100.0;
    }
}
