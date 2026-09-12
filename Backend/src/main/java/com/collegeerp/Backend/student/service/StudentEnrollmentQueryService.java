package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.schoolclass.dto.ClassEnrollmentResponse;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Student self-service enrollment view backed only by ClassEnrollment. */
@Service
@Transactional(readOnly = true)
public class StudentEnrollmentQueryService {

    private final com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository classEnrollmentRepository;

    public StudentEnrollmentQueryService(
            com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository classEnrollmentRepository) {
        this.classEnrollmentRepository = classEnrollmentRepository;
    }

    public List<ClassEnrollmentResponse> getEnrollments(Long studentId) {
        return classEnrollmentRepository.findAllByStudentId(studentId).stream()
                .map(this::map)
                .toList();
    }

    private ClassEnrollmentResponse map(ClassEnrollment enrollment) {
        ClassSubject classSubject = enrollment.getClassSubject();
        var student = enrollment.getStudent();
        var subject = classSubject.getSubject();

        return ClassEnrollmentResponse.builder()
                .id(enrollment.getId())
                .classSubjectId(classSubject.getId())
                .schoolClassId(classSubject.getSchoolClass() != null ? classSubject.getSchoolClass().getId() : null)
                .className(classSubject.getSchoolClass() != null ? classSubject.getSchoolClass().getName() : null)
                .academicYear(classSubject.getSchoolClass() != null ? classSubject.getSchoolClass().getAcademicYear() : null)
                .semester(classSubject.getSchoolClass() != null ? classSubject.getSchoolClass().getSemester() : null)
                .teacherId(classSubject.getTeacher() != null ? classSubject.getTeacher().getId() : null)
                .teacherName(classSubject.getTeacher() != null
                        ? (classSubject.getTeacher().getFirstName() + " " +
                           (classSubject.getTeacher().getLastName() != null ? classSubject.getTeacher().getLastName() : "")).trim()
                        : null)
                .subjectId(subject != null ? subject.getId() : null)
                .subjectName(subject != null ? subject.getSubjectName() : classSubject.getSubjectName())
                .subjectCode(subject != null ? subject.getSubjectCode() : classSubject.getSubjectCode())
                .courseName(subject != null && subject.getCourse() != null ? subject.getCourse().getCourseName() : null)
                .studentId(student.getId())
                .studentName((student.getFirstName() + " " + (student.getLastName() != null ? student.getLastName() : "")).trim())
                .admissionNo(student.getAdmissionNo())
                .source(enrollment.getSource())
                .enrolledAt(enrollment.getEnrolledAt())
                .enrollmentDate(enrollment.getEnrolledAt() != null ? enrollment.getEnrolledAt().toLocalDate() : LocalDate.now())
                .status("ACTIVE")
                .build();
    }
}
