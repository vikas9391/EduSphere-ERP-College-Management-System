package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.enrollment.dto.EnrollmentResponse;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Student self-service enrollment view backed by the authoritative ClassEnrollment model.
 * Legacy Enrollment is intentionally not used for the current student subject list.
 */
@Service
@Transactional(readOnly = true)
public class StudentEnrollmentQueryService {

    private final com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository classEnrollmentRepository;

    public StudentEnrollmentQueryService(
            com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository classEnrollmentRepository) {
        this.classEnrollmentRepository = classEnrollmentRepository;
    }

    public List<EnrollmentResponse> getEnrollments(Long studentId) {
        return classEnrollmentRepository.findAllByStudentId(studentId).stream()
                .map(this::map)
                .toList();
    }

    private EnrollmentResponse map(ClassEnrollment enrollment) {
        ClassSubject classSubject = enrollment.getClassSubject();
        var student = enrollment.getStudent();
        var subject = classSubject.getSubject();

        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + (student.getLastName() != null ? student.getLastName() : ""))
                .admissionNo(student.getAdmissionNo())
                .subjectId(subject != null ? subject.getId() : null)
                .subjectName(subject != null ? subject.getSubjectName() : classSubject.getSubjectName())
                .subjectCode(subject != null ? subject.getSubjectCode() : classSubject.getSubjectCode())
                .courseName(subject != null && subject.getCourse() != null
                        ? subject.getCourse().getCourseName() : null)
                .teacherName(classSubject.getTeacher() != null
                        ? classSubject.getTeacher().getFirstName() + " " +
                          (classSubject.getTeacher().getLastName() != null ? classSubject.getTeacher().getLastName() : "")
                        : null)
                .academicYear(classSubject.getSchoolClass().getAcademicYear())
                .semester(classSubject.getSchoolClass().getSemester())
                .enrollmentDate(enrollment.getEnrolledAt() != null
                        ? enrollment.getEnrolledAt().toLocalDate() : LocalDate.now())
                .status("ACTIVE")
                .build();
    }
}
