package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.assignment.entity.Assignment;
import com.collegeerp.Backend.assignment.repository.AssignmentRepository;
import com.collegeerp.Backend.assignment.repository.AssignmentSubmissionRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.student.entity.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAssignmentServiceTest {

    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentSubmissionRepository submissionRepository;

    @Test
    void assignmentsAreLoadedOnlyForTheStudentsClassSubjects() {
        ClassSubject classA = ClassSubject.builder().id(101L).subjectName("Mathematics A").build();
        ClassSubject classB = ClassSubject.builder().id(202L).subjectName("Mathematics B").build();

        ClassEnrollment enrollmentA = ClassEnrollment.builder()
                .id(1L)
                .student(Student.builder().id(1L).build())
                .classSubject(classA)
                .build();
        Assignment assignmentA = Assignment.builder()
                .id(11L)
                .classSubject(classA)
                .title("Class A Assignment")
                .build();

        when(classEnrollmentRepository.findAllByStudentId(1L)).thenReturn(List.of(enrollmentA));
        when(assignmentRepository.findForStudentClassSubjects(List.of(101L))).thenReturn(List.of(assignmentA));
        when(submissionRepository.findByStudentId(1L)).thenReturn(List.of());

        var assignments = new StudentAssignmentService(
                classEnrollmentRepository,
                assignmentRepository,
                submissionRepository
        ).getAssignments(1L);

        assertEquals(List.of(11L), assignments.stream().map(r -> r.getAssignmentId()).toList());
        verify(assignmentRepository).findForStudentClassSubjects(List.of(101L));
        verify(assignmentRepository, org.mockito.Mockito.never())
                .findForStudentClassSubjects(List.of(202L));
    }

    @Test
    void studentWithNoClassEnrollmentReceivesNoAssignments() {
        when(classEnrollmentRepository.findAllByStudentId(2L)).thenReturn(List.of());

        var assignments = new StudentAssignmentService(
                classEnrollmentRepository,
                assignmentRepository,
                submissionRepository
        ).getAssignments(2L);

        assertEquals(List.of(), assignments);
        verify(assignmentRepository, org.mockito.Mockito.never()).findForStudentClassSubjects(anyList());
    }
}
