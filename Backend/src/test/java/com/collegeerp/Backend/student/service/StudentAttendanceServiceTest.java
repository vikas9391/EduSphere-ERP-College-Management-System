package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.attendance.entity.Attendance;
import com.collegeerp.Backend.attendance.repository.AttendanceRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.student.dto.StudentAttendanceResponse;
import com.collegeerp.Backend.subject.entity.Subject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAttendanceServiceTest {

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;

    @Test
    void classBasedAttendanceIsResolvedThroughClassEnrollmentAndSubject() {
        Subject subject = subject(10L, "JAVA", "Java");
        ClassEnrollment classEnrollment = classEnrollment(30L, 20L, subject);

        Attendance present = Attendance.builder()
                .id(40L).classEnrollment(classEnrollment)
                .attendanceDate(LocalDate.of(2026, 9, 1)).status("PRESENT").build();
        Attendance absent = Attendance.builder()
                .id(41L).classEnrollment(classEnrollment)
                .attendanceDate(LocalDate.of(2026, 9, 2)).status("ABSENT").build();

        when(classEnrollmentRepository.findAllByStudentId(1L)).thenReturn(List.of(classEnrollment));
        when(attendanceRepository.findClassAttendanceByStudentId(1L)).thenReturn(List.of(present, absent));
        when(attendanceRepository.findLegacyAttendanceByStudentId(1L)).thenReturn(List.of());

        StudentAttendanceResponse response = service().getAttendance(1L);

        assertEquals(2, response.getTotalClasses());
        assertEquals(1, response.getClassesAttended());
        assertEquals(1, response.getClassesMissed());
        assertEquals(50.0, response.getOverallAttendancePercentage());
        assertEquals(1, response.getBySubject().size());
        assertEquals("JAVA", response.getBySubject().get(0).getSubjectCode());
    }

    @Test
    void currentSubjectWithNoAttendanceStillAppears() {
        Subject subject = subject(12L, "CHAIN", "Blockchain");
        ClassEnrollment classEnrollment = classEnrollment(32L, 22L, subject);

        when(classEnrollmentRepository.findAllByStudentId(1L)).thenReturn(List.of(classEnrollment));
        when(attendanceRepository.findClassAttendanceByStudentId(1L)).thenReturn(List.of());
        when(attendanceRepository.findLegacyAttendanceByStudentId(1L)).thenReturn(List.of());

        StudentAttendanceResponse response = service().getAttendance(1L);

        assertEquals(0, response.getTotalClasses());
        assertEquals(1, response.getBySubject().size());
        assertEquals("CHAIN", response.getBySubject().get(0).getSubjectCode());
        assertEquals(0, response.getBySubject().get(0).getTotalClasses());
        assertEquals(0.0, response.getBySubject().get(0).getAttendancePercentage());
    }

    @Test
    void lateCountsAttendedAndExcusedIsExcludedFromDenominator() {
        Subject subject = subject(13L, "OS", "Operating Systems");
        ClassEnrollment classEnrollment = classEnrollment(33L, 23L, subject);

        Attendance late = Attendance.builder()
                .id(70L).classEnrollment(classEnrollment)
                .attendanceDate(LocalDate.of(2026, 9, 1)).status("LATE").build();
        Attendance excused = Attendance.builder()
                .id(71L).classEnrollment(classEnrollment)
                .attendanceDate(LocalDate.of(2026, 9, 2)).status("EXCUSED").build();

        when(classEnrollmentRepository.findAllByStudentId(1L)).thenReturn(List.of(classEnrollment));
        when(attendanceRepository.findClassAttendanceByStudentId(1L)).thenReturn(List.of(late, excused));
        when(attendanceRepository.findLegacyAttendanceByStudentId(1L)).thenReturn(List.of());

        StudentAttendanceResponse response = service().getAttendance(1L);

        assertEquals(1, response.getTotalClasses());
        assertEquals(1, response.getClassesAttended());
        assertEquals(0, response.getClassesMissed());
        assertEquals(100.0, response.getOverallAttendancePercentage());
    }

    @Test
    void migratedClassRowWinsOverMatchingLegacyRow() {
        Subject subject = subject(14L, "DBMS", "DBMS");
        ClassEnrollment classEnrollment = classEnrollment(34L, 24L, subject);
        var legacyEnrollment = com.collegeerp.Backend.enrollment.entity.Enrollment.builder()
                .id(50L).subject(subject).build();

        Attendance classPresent = Attendance.builder()
                .id(80L).classEnrollment(classEnrollment)
                .attendanceDate(LocalDate.of(2026, 9, 1)).status("PRESENT").build();
        Attendance legacyAbsent = Attendance.builder()
                .id(81L).enrollment(legacyEnrollment)
                .attendanceDate(LocalDate.of(2026, 9, 1)).status("ABSENT").build();

        when(classEnrollmentRepository.findAllByStudentId(1L)).thenReturn(List.of(classEnrollment));
        when(attendanceRepository.findClassAttendanceByStudentId(1L)).thenReturn(List.of(classPresent));
        when(attendanceRepository.findLegacyAttendanceByStudentId(1L)).thenReturn(List.of(legacyAbsent));

        StudentAttendanceResponse response = service().getAttendance(1L);

        assertEquals(1, response.getTotalClasses());
        assertEquals(1, response.getClassesAttended());
        assertEquals(100.0, response.getOverallAttendancePercentage());
    }

    @Test
    void legacyAttendanceStillWorksDuringMigration() {
        Subject subject = subject(11L, "DBMS", "DBMS");
        var enrollment = com.collegeerp.Backend.enrollment.entity.Enrollment.builder()
                .id(50L).subject(subject).build();
        Attendance present = Attendance.builder()
                .id(60L).enrollment(enrollment)
                .attendanceDate(LocalDate.of(2026, 9, 1)).status("PRESENT").build();

        when(classEnrollmentRepository.findAllByStudentId(1L)).thenReturn(List.of());
        when(attendanceRepository.findClassAttendanceByStudentId(1L)).thenReturn(List.of());
        when(attendanceRepository.findLegacyAttendanceByStudentId(1L)).thenReturn(List.of(present));

        StudentAttendanceResponse response = service().getAttendance(1L);

        assertEquals(1, response.getTotalClasses());
        assertEquals(1, response.getClassesAttended());
        assertEquals("DBMS", response.getBySubject().get(0).getSubjectCode());
    }

    private StudentAttendanceService service() {
        return new StudentAttendanceService(attendanceRepository, classEnrollmentRepository);
    }

    private Subject subject(Long id, String code, String name) {
        return Subject.builder().id(id).subjectCode(code).subjectName(name).build();
    }

    private ClassEnrollment classEnrollment(Long enrollmentId, Long classSubjectId, Subject subject) {
        ClassSubject classSubject = ClassSubject.builder()
                .id(classSubjectId)
                .subject(subject)
                .subjectCode(subject.getSubjectCode())
                .subjectName(subject.getSubjectName())
                .build();
        return ClassEnrollment.builder().id(enrollmentId).classSubject(classSubject).build();
    }
}
