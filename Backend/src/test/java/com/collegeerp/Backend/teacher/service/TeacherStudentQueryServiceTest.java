package com.collegeerp.Backend.teacher.service;

import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.entity.SchoolClass;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.student.entity.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherStudentQueryServiceTest {

    @Mock
    private ClassEnrollmentRepository classEnrollmentRepository;

    @Test
    void returnsOnlyExactClassEnrollmentRoster() {
        User teacher = User.builder().id(7L).firstName("Rahul").lastName("Teacher").build();
        SchoolClass schoolClass = SchoolClass.builder()
                .id(11L).name("CSE-A").academicYear("2026-27").semester(3).build();
        ClassSubject classSubject = ClassSubject.builder()
                .id(21L)
                .schoolClass(schoolClass)
                .teacher(teacher)
                .subjectCode("CS301")
                .subjectName("Algorithms")
                .build();
        Student student = Student.builder()
                .id(31L).firstName("Vikas").lastName("Student").admissionNo("ADM-31").build();
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(41L)
                .classSubject(classSubject)
                .student(student)
                .enrolledAt(LocalDateTime.of(2026, 8, 1, 10, 0))
                .build();

        when(classEnrollmentRepository.findAllByTeacherId(7L)).thenReturn(List.of(enrollment));

        var response = new TeacherStudentQueryService(classEnrollmentRepository).getStudents(7L);

        assertEquals(1, response.size());
        assertEquals(31L, response.get(0).getStudentId());
        assertEquals(21L, response.get(0).getSubjectId());
        assertEquals("Algorithms", response.get(0).getSubjectName());
        assertEquals("CSE-A", response.get(0).getCourseName());
        assertEquals("2026-27", response.get(0).getAcademicYear());
        assertEquals(3, response.get(0).getSemester());
        verify(classEnrollmentRepository).findAllByTeacherId(7L);
    }
}
