package com.collegeerp.Backend.attendance.service;

import com.collegeerp.Backend.attendance.dto.StudentAttendanceSummaryResponse;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.student.service.StudentAttendanceService;
import com.collegeerp.Backend.student.service.StudentIdentityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reuses the canonical student attendance calculation for the legacy summary endpoint. */
@Service
@Transactional(readOnly = true)
public class StudentAttendanceSummaryQueryService {

    private final StudentIdentityService studentIdentityService;
    private final StudentAttendanceService studentAttendanceService;

    public StudentAttendanceSummaryQueryService(
            StudentIdentityService studentIdentityService,
            StudentAttendanceService studentAttendanceService) {
        this.studentIdentityService = studentIdentityService;
        this.studentAttendanceService = studentAttendanceService;
    }

    public StudentAttendanceSummaryResponse getSummary(UserPrincipal principal) {
        Long studentId = studentIdentityService.requireStudentId(principal);
        var source = studentAttendanceService.getAttendance(studentId);

        return StudentAttendanceSummaryResponse.builder()
                .totalClasses(Math.toIntExact(source.getTotalClasses()))
                .classesAttended(Math.toIntExact(source.getClassesAttended()))
                .classesMissed(Math.toIntExact(source.getClassesMissed()))
                .overallAttendancePercentage(source.getOverallAttendancePercentage())
                .bySubject(source.getBySubject().stream()
                        .map(s -> StudentAttendanceSummaryResponse.SubjectAttendanceSummary.builder()
                                .subjectId(s.getSubjectId())
                                .subjectCode(s.getSubjectCode())
                                .subjectName(s.getSubjectName())
                                .totalClasses(Math.toIntExact(s.getTotalClasses()))
                                .classesAttended(Math.toIntExact(s.getClassesAttended()))
                                .classesMissed(Math.toIntExact(s.getClassesMissed()))
                                .attendancePercentage(s.getAttendancePercentage())
                                .build())
                        .toList())
                .build();
    }
}
