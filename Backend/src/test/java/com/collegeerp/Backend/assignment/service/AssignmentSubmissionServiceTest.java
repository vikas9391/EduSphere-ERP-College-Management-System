package com.collegeerp.Backend.assignment.service;

import com.collegeerp.Backend.assignment.dto.AssignmentSubmissionRequest;
import com.collegeerp.Backend.assignment.entity.Assignment;
import com.collegeerp.Backend.assignment.entity.AssignmentSubmission;
import com.collegeerp.Backend.assignment.repository.AssignmentRepository;
import com.collegeerp.Backend.assignment.repository.AssignmentSubmissionRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentSubmissionServiceTest {

    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;

    @Test
    void rejectsStudentOutsideAssignmentsClassSubject() {
        ClassSubject classSubject = ClassSubject.builder().id(20L).build();
        Assignment assignment = Assignment.builder()
                .id(10L)
                .classSubject(classSubject)
                .title("Class A Assignment")
                .build();
        Student student = Student.builder().id(30L).firstName("Rahul").lastName("B").build();

        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(studentRepository.findById(30L)).thenReturn(Optional.of(student));
        when(submissionRepository.existsByAssignmentIdAndStudentId(10L, 30L)).thenReturn(false);
        when(classEnrollmentRepository.existsByClassSubjectIdAndStudentId(20L, 30L)).thenReturn(false);

        AssignmentSubmissionService service = service();
        AssignmentSubmissionRequest request = AssignmentSubmissionRequest.builder()
                .assignmentId(10L)
                .studentId(30L)
                .submissionUrl("https://example.com/work")
                .build();

        assertThrows(AccessDeniedException.class, () -> service.submitAssignment(request));
    }

    @Test
    void acceptsStudentWithExactClassEnrollment() {
        ClassSubject classSubject = ClassSubject.builder().id(20L).build();
        Assignment assignment = Assignment.builder()
                .id(10L)
                .classSubject(classSubject)
                .title("Class A Assignment")
                .build();
        Student student = Student.builder().id(30L).firstName("Vikas").lastName("A").build();

        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(studentRepository.findById(30L)).thenReturn(Optional.of(student));
        when(submissionRepository.existsByAssignmentIdAndStudentId(10L, 30L)).thenReturn(false);
        when(classEnrollmentRepository.existsByClassSubjectIdAndStudentId(20L, 30L)).thenReturn(true);
        when(submissionRepository.save(any(AssignmentSubmission.class)))
                .thenAnswer(invocation -> {
                    AssignmentSubmission saved = invocation.getArgument(0);
                    saved.setId(40L);
                    return saved;
                });

        AssignmentSubmissionRequest request = AssignmentSubmissionRequest.builder()
                .assignmentId(10L)
                .studentId(30L)
                .submissionUrl("https://example.com/work")
                .build();

        var response = service().submitAssignment(request);
        assertEquals(40L, response.getId());
        assertEquals(30L, response.getStudentId());
        assertEquals("SUBMITTED", response.getStatus());
    }

    private AssignmentSubmissionService service() {
        return new AssignmentSubmissionService(
                submissionRepository,
                assignmentRepository,
                studentRepository,
                classEnrollmentRepository);
    }
}
