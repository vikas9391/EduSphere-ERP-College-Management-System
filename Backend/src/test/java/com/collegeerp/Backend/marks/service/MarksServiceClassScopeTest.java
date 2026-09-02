package com.collegeerp.Backend.marks.service;

import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.examination.entity.Exam;
import com.collegeerp.Backend.examination.entity.ExamSchedule;
import com.collegeerp.Backend.examination.repository.ExamScheduleRepository;
import com.collegeerp.Backend.marks.dto.MarksRequest;
import com.collegeerp.Backend.marks.repository.MarksRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import com.collegeerp.Backend.subject.entity.Subject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarksServiceClassScopeTest {

    @Mock private MarksRepository marksRepository;
    @Mock private ExamScheduleRepository examScheduleRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsMarksForStudentOutsideExactClassSubject() {
        User teacher = User.builder().id(7L).firstName("Teacher").lastName("A").build();
        Subject subject = Subject.builder()
                .id(10L).subjectCode("JAVA").subjectName("Java").credits(4).teacher(teacher).build();
        ClassSubject classSubject = ClassSubject.builder()
                .id(20L).subject(subject).subjectCode("JAVA").subjectName("Java").teacher(teacher).build();
        Exam exam = Exam.builder().id(30L).examName("Midterm").semester(1).academicYear("2026-27").build();
        ExamSchedule schedule = ExamSchedule.builder()
                .id(40L).exam(exam).subject(subject).classSubject(classSubject).maxMarks(100).build();
        Student student = Student.builder().id(50L).firstName("Rahul").lastName("B").build();

        when(examScheduleRepository.findByIdWithDetails(40L)).thenReturn(Optional.of(schedule));
        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(marksRepository.existsByExamScheduleIdAndStudentId(40L, 50L)).thenReturn(false);
        when(classEnrollmentRepository.findByClassSubjectIdAndStudentId(20L, 50L)).thenReturn(Optional.empty());

        UserPrincipal principal = new UserPrincipal(7L, "teacher@example.com", "TEACHER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));

        MarksRequest request = MarksRequest.builder()
                .examScheduleId(40L)
                .studentId(50L)
                .internalMarks(20)
                .externalMarks(50)
                .build();

        assertThrows(BadRequestException.class, () -> service().enterMarks(request));
    }

    private MarksService service() {
        return new MarksService(
                marksRepository,
                examScheduleRepository,
                studentRepository,
                classEnrollmentRepository);
    }
}
