package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.attendance.entity.Attendance;
import com.collegeerp.Backend.attendance.repository.AttendanceRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.student.dto.StudentAttendanceResponse;
import com.collegeerp.Backend.subject.entity.Subject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Test
    void classBasedAttendanceIsResolvedThroughClassEnrollmentAndSubject() {
        Subject subject = Subject.builder()
                .id(10L)
                .subjectCode("JAVA")
                .subjectName("Java")
                .build();

        ClassSubject classSubject = ClassSubject.builder()
                .id(20L)
                .subject(subject)
                .subjectCode("JAVA")
                .subjectName("Java")
                .build();

        ClassEnrollment classEnrollment = ClassEnrollment.builder()
                .id(30L)
                .classSubject(classSubject)
                .build();

        Attendance present = Attendance.builder()
                .id(40L)
                .classEnrollment(classEnrollment)
                .attendanceDate(LocalDate.of(2026, 9, 1))
                .status("PRESENT")
                .build();

        Attendance absent = Attendance.builder()
                .id(41L)
                .classEnrollment(classEnrollment)
                .attendanceDate(LocalDate.of(2026, 9, 2))
                .status("ABSENT")
                .build();

        when(attendanceRepository.findClassAttendanceByStudentId(1L)).thenReturn(List.of(present, absent));
        when(attendanceRepository.findLegacyAttendanceByStudentId(1L)).thenReturn(List.of());

        StudentAttendanceResponse response = new StudentAttendanceService(attendanceRepository).getAttendance(1L);

        assertEquals(2, response.getTotalClasses());
        assertEquals(1, response.getClassesAttended());
        assertEquals(1, response.getClassesMissed());
        assertEquals(50.0, response.getOverallAttendancePercentage());
        assertEquals(1, response.getBySubject().size());
        assertEquals("JAVA", response.getBySubject().get(0).getSubjectCode());
        assertEquals(50.0, response.getBySubject().get(0).getAttendancePercentage());
    }

    @Test
    void legacyAttendanceStillWorksDuringMigration() {
        Subject subject = Subject.builder()
                .id(11L)
                .subjectCode("DBMS")
                .subjectName("DBMS")
                .build();

        var enrollment = com.collegeerp.Backend.enrollment.entity.Enrollment.builder()
                .id(50L)
                .subject(subject)
                .build();

        Attendance present = Attendance.builder()
                .id(60L)
                .enrollment(enrollment)
                .attendanceDate(LocalDate.of(2026, 9, 1))
                .status("PRESENT")
                .build();

        when(attendanceRepository.findClassAttendanceByStudentId(1L)).thenReturn(List.of());
        when(attendanceRepository.findLegacyAttendanceByStudentId(1L)).thenReturn(List.of(present));

        StudentAttendanceResponse response = new StudentAttendanceService(attendanceRepository).getAttendance(1L);

        assertEquals(1, response.getTotalClasses());
        assertEquals(1, response.getClassesAttended());
        assertEquals("DBMS", response.getBySubject().get(0).getSubjectCode());
    }
}
