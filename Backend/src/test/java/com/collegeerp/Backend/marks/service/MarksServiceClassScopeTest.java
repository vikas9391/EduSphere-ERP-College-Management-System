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
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsMarksForStudentOutsideExactClassSubject() {
        User teacher = User.builder().id(7L).firstName("Teacher").lastName("A").build();
        ClassSubject classSubject = ClassSubject.builder()
                .id(20L).subjectCode("JAVA").subjectName("Java").teacher(teacher).build();
        Exam exam = Exam.builder().id(30L).examName("Midterm").semester(1).academicYear("2026-27").build();
        ExamSchedule schedule = ExamSchedule.builder()
                .id(40L).exam(exam).classSubject(classSubject).maxMarks(100).build();

        when(examScheduleRepository.findByIdWithDetails(40L)).thenReturn(Optional.of(schedule));
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
        return new MarksService(marksRepository, examScheduleRepository, classEnrollmentRepository);
    }
}
