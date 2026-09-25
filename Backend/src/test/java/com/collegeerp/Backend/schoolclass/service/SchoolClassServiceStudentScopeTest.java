package com.collegeerp.Backend.schoolclass.service;

import com.collegeerp.Backend.schoolclass.entity.ClassStudent;
import com.collegeerp.Backend.schoolclass.entity.SchoolClass;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.schoolclass.repository.ClassStudentRepository;
import com.collegeerp.Backend.schoolclass.repository.ClassSubjectRepository;
import com.collegeerp.Backend.schoolclass.repository.SchoolClassRepository;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolClassServiceStudentScopeTest {

    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private ClassSubjectRepository classSubjectRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private StudentRepository studentRepository;

    @Test
    void eachStudentSeesOnlyClassesFromTheirOwnClassStudentMemberships() {
        SchoolClass classA = schoolClass(101L, "CSE-A");
        SchoolClass classB = schoolClass(202L, "CSE-B");

        ClassStudent studentAInClassA = ClassStudent.builder()
                .schoolClass(classA)
                .student(Student.builder().id(1L).build())
                .build();
        ClassStudent studentBInClassB = ClassStudent.builder()
                .schoolClass(classB)
                .student(Student.builder().id(2L).build())
                .build();

        when(classStudentRepository.findAllByStudentId(1L)).thenReturn(List.of(studentAInClassA));
        when(classStudentRepository.findAllByStudentId(2L)).thenReturn(List.of(studentBInClassB));
        when(classStudentRepository.findAllByClassId(101L)).thenReturn(List.of(studentAInClassA));
        when(classStudentRepository.findAllByClassId(202L)).thenReturn(List.of(studentBInClassB));
        when(classSubjectRepository.countBySchoolClassId(101L)).thenReturn(4);
        when(classSubjectRepository.countBySchoolClassId(202L)).thenReturn(5);

        SchoolClassService service = new SchoolClassService(
                schoolClassRepository,
                classStudentRepository,
                classSubjectRepository,
                classEnrollmentRepository,
                userRepository,
                studentRepository
        );

        var classesForA = service.getMyClassesAsStudent(1L, "STUDENT");
        var classesForB = service.getMyClassesAsStudent(2L, "STUDENT");

        assertEquals(List.of(101L), classesForA.stream().map(r -> r.getId()).toList());
        assertEquals(List.of(202L), classesForB.stream().map(r -> r.getId()).toList());
    }

    @Test
    void nonStudentCannotUseStudentClassScopeEndpoint() {
        SchoolClassService service = new SchoolClassService(
                schoolClassRepository,
                classStudentRepository,
                classSubjectRepository,
                classEnrollmentRepository,
                userRepository,
                studentRepository
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                com.collegeerp.Backend.common.exception.ForbiddenException.class,
                () -> service.getMyClassesAsStudent(1L, "TEACHER")
        );
    }

    private SchoolClass schoolClass(Long id, String name) {
        return SchoolClass.builder()
                .id(id)
                .name(name)
                .teacher(User.builder().id(10L).firstName("Teacher").lastName("One").build())
                .build();
    }
}
