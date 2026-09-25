package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.assignment.entity.Assignment;
import com.collegeerp.Backend.assignment.repository.AssignmentRepository;
import com.collegeerp.Backend.assignment.repository.AssignmentSubmissionRepository;
import com.collegeerp.Backend.attendance.repository.AttendanceRepository;
import com.collegeerp.Backend.examination.repository.ExamScheduleRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.entity.SchoolClass;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.student.dto.StudentDashboardResponse;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import com.collegeerp.Backend.subject.entity.Subject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentDashboardServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private ExamScheduleRepository examScheduleRepository;
    @Mock private StudentResultService studentResultService;
    @Mock private StudentNotificationService studentNotificationService;

    @Test
    void dashboardUsesOnlyClassSubjectsBelongingToRequestedStudent() {
        Student student = Student.builder().id(1L).firstName("Student").lastName("A").build();
        Subject java = Subject.builder().id(10L).subjectCode("JAVA").subjectName("Java").build();
        Subject python = Subject.builder().id(20L).subjectCode("PY").subjectName("Python").build();

        SchoolClass classA = SchoolClass.builder().id(100L).name("CSE-A").semester(1).build();
        SchoolClass classB = SchoolClass.builder().id(200L).name("CSE-B").semester(1).build();

        ClassSubject subjectA = ClassSubject.builder().id(101L).schoolClass(classA).subject(java).build();
        ClassSubject subjectB = ClassSubject.builder().id(202L).schoolClass(classB).subject(python).build();

        ClassEnrollment enrollmentA = ClassEnrollment.builder()
                .id(301L).student(student).classSubject(subjectA).build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(classEnrollmentRepository.findAllByStudentId(1L)).thenReturn(List.of(enrollmentA));
        when(attendanceRepository.findClassAttendanceByStudentId(1L)).thenReturn(List.of());
        when(assignmentRepository.findForStudentClassSubjects(List.of(101L))).thenReturn(List.of());
        when(submissionRepository.findByStudentId(1L)).thenReturn(List.of());
        when(examScheduleRepository.findUpcomingForStudent(eq(List.of(101L)), any())).thenReturn(List.of());
        when(studentResultService.getResults(1L)).thenReturn(
                com.collegeerp.Backend.result.dto.OverallResultResponse.builder()
                        .studentId(1L).cgpa(8.0).semesterResults(List.of()).totalCredits(0).build());
        when(studentNotificationService.getUnreadCount(1L)).thenReturn(0L);

        StudentDashboardResponse response = service().getDashboard(1L);

        assertEquals(1, response.getTotalSubjects());
        assertEquals(0, response.getPendingAssignments());
        assertEquals(0, response.getUpcomingExams());
        assertEquals(8.0, response.getCgpa());

        verify(assignmentRepository).findForStudentClassSubjects(List.of(101L));
        verify(examScheduleRepository).findUpcomingForStudent(eq(List.of(101L)), any());
        verify(assignmentRepository, never()).findForStudentClassSubjects(List.of(202L));
        verify(examScheduleRepository, never()).findUpcomingForStudent(eq(List.of(202L)), any());
    }

    @Test
    void dashboardDoesNotQueryClassScopedDataWhenStudentHasNoClassEnrollment() {
        Student student = Student.builder().id(1L).firstName("Student").build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(classEnrollmentRepository.findAllByStudentId(1L)).thenReturn(List.of());
        when(attendanceRepository.findClassAttendanceByStudentId(1L)).thenReturn(List.of());
        when(studentResultService.getResults(1L)).thenReturn(
                com.collegeerp.Backend.result.dto.OverallResultResponse.builder()
                        .studentId(1L).cgpa(0.0).semesterResults(List.of()).totalCredits(0).build());
        when(studentNotificationService.getUnreadCount(1L)).thenReturn(0L);

        StudentDashboardResponse response = service().getDashboard(1L);

        assertEquals(0, response.getTotalSubjects());
        assertEquals(0, response.getPendingAssignments());
        assertEquals(0, response.getUpcomingExams());
        verify(assignmentRepository, never()).findForStudentClassSubjects(anyList());
        verify(examScheduleRepository, never()).findUpcomingForStudent(anyList(), any());
    }

    private StudentDashboardService service() {
        return new StudentDashboardService(
                studentRepository,
                classEnrollmentRepository,
                attendanceRepository,
                assignmentRepository,
                submissionRepository,
                examScheduleRepository,
                studentResultService,
                studentNotificationService
        );
    }
}
