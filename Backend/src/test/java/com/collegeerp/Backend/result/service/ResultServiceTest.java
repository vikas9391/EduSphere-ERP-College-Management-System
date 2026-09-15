package com.collegeerp.Backend.result.service;

import com.collegeerp.Backend.examination.entity.Exam;
import com.collegeerp.Backend.examination.entity.ExamSchedule;
import com.collegeerp.Backend.marks.entity.Marks;
import com.collegeerp.Backend.marks.repository.MarksRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.entity.SchoolClass;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock private MarksRepository marksRepository;
    @Mock private StudentRepository studentRepository;

    @Test
    void overallResultUsesClassScopedPublishedMarksForRequestedStudent() {
        Student student = Student.builder().id(1L).firstName("Vikas").lastName("A").build();
        SchoolClass schoolClass = SchoolClass.builder()
                .id(2L).name("CSE-A").academicYear("2026-27").semester(1).build();
        Subject java = Subject.builder()
                .id(10L).subjectCode("JAVA").subjectName("Java").credits(4).semester(1).build();
        ClassSubject classSubject = ClassSubject.builder()
                .id(11L).schoolClass(schoolClass).subjectCode("JAVA").subjectName("Java")
                .credits(4).subject(java).build();
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(12L).classSubject(classSubject).student(student)
                .source(ClassEnrollment.Source.AUTO).build();
        Exam exam = Exam.builder()
                .id(20L).examName("Semester 1").examType("SEMESTER")
                .semester(1).academicYear("2026-27").build();
        ExamSchedule schedule = ExamSchedule.builder()
                .id(30L).exam(exam).classSubject(classSubject).maxMarks(100).build();
        Marks published = Marks.builder()
                .id(40L).classEnrollment(enrollment).examSchedule(schedule)
                .internalMarks(20).externalMarks(60).totalMarks(80)
                .grade("A").gradePoint(8.0).status("PUBLISHED").build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(marksRepository.findAllPublishedByStudent(1L)).thenReturn(List.of(published));

        var response = new ResultService(marksRepository, studentRepository).getOverallResult(1L);

        assertEquals(1L, response.getStudentId());
        assertEquals(4, response.getTotalCredits());
        assertEquals(8.0, response.getCgpa());
        assertEquals(1, response.getSemesterResults().size());
        assertEquals(1, response.getSemesterResults().get(0).getSubjects().size());
        assertEquals("JAVA", response.getSemesterResults().get(0).getSubjects().get(0).getSubjectCode());
        verify(marksRepository).findAllPublishedByStudent(1L);
    }
}
