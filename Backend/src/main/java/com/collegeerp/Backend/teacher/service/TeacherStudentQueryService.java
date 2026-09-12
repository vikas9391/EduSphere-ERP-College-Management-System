package com.collegeerp.Backend.teacher.service;

import com.collegeerp.Backend.schoolclass.dto.ClassEnrollmentResponse;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Teacher-scoped roster derived only from ClassEnrollment relationships. */
@Service
@Transactional(readOnly = true)
public class TeacherStudentQueryService {

    private final ClassEnrollmentRepository classEnrollmentRepository;

    public TeacherStudentQueryService(ClassEnrollmentRepository classEnrollmentRepository) {
        this.classEnrollmentRepository = classEnrollmentRepository;
    }

    public List<ClassEnrollmentResponse> getStudents(Long teacherId) {
        return classEnrollmentRepository.findAllByTeacherId(teacherId)
                .stream()
                .map(this::map)
                .toList();
    }

    private ClassEnrollmentResponse map(ClassEnrollment e) {
        var cs = e.getClassSubject();
        var formalSubject = cs.getSubject();
        var schoolClass = cs.getSchoolClass();
        var teacher = cs.getTeacher();
        var student = e.getStudent();

        String courseName = formalSubject != null && formalSubject.getCourse() != null
                ? formalSubject.getCourse().getCourseName()
                : schoolClass.getName();

        return ClassEnrollmentResponse.builder()
                .id(e.getId())
                .classSubjectId(cs.getId())
                .schoolClassId(schoolClass != null ? schoolClass.getId() : null)
                .className(schoolClass != null ? schoolClass.getName() : null)
                .academicYear(schoolClass != null ? schoolClass.getAcademicYear() : null)
                .semester(schoolClass != null ? schoolClass.getSemester() : null)
                .teacherId(teacher != null ? teacher.getId() : null)
                .teacherName(teacher != null ? (teacher.getFirstName() + " " + (teacher.getLastName() != null ? teacher.getLastName() : "")).trim() : null)
                .subjectId(formalSubject != null ? formalSubject.getId() : cs.getId())
                .subjectName(cs.getSubjectName())
                .subjectCode(cs.getSubjectCode())
                .courseName(courseName)
                .studentId(student.getId())
                .studentName((student.getFirstName() + " " + (student.getLastName() != null ? student.getLastName() : "")).trim())
                .admissionNo(student.getAdmissionNo())
                .source(e.getSource())
                .enrolledAt(e.getEnrolledAt())
                .enrollmentDate(e.getEnrolledAt() != null ? e.getEnrolledAt().toLocalDate() : null)
                .status("ACTIVE")
                .build();
    }
}
