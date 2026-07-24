package com.collegeerp.Backend.teacher.service;

import com.collegeerp.Backend.enrollment.dto.EnrollmentResponse;
import com.collegeerp.Backend.enrollment.entity.Enrollment;
import com.collegeerp.Backend.enrollment.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only, teacher-scoped roster over {@link Enrollment} - every student enrolled in every
 * subject this teacher owns. Reuses {@link EnrollmentResponse}, same rationale as
 * {@code StudentEnrollmentQueryService}. One row per (student, subject) pair; a student
 * enrolled in two of this teacher's subjects appears twice, which is correct for a roster.
 */
@Service
@Transactional(readOnly = true)
public class TeacherStudentQueryService {

    private final EnrollmentRepository enrollmentRepository;

    public TeacherStudentQueryService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<EnrollmentResponse> getStudents(Long teacherId) {
        return enrollmentRepository.findBySubjectTeacherIdWithDetails(teacherId)
                .stream()
                .map(this::map)
                .toList();
    }

    private EnrollmentResponse map(Enrollment e) {
        return EnrollmentResponse.builder()
                .id(e.getId())
                .studentId(e.getStudent().getId())
                .studentName(e.getStudent().getFirstName() + " " + e.getStudent().getLastName())
                .admissionNo(e.getStudent().getAdmissionNo())
                .subjectId(e.getSubject().getId())
                .subjectName(e.getSubject().getSubjectName())
                .subjectCode(e.getSubject().getSubjectCode())
                .courseName(e.getSubject().getCourse().getCourseName())
                .teacherName(e.getSubject().getTeacher().getFirstName() + " " + e.getSubject().getTeacher().getLastName())
                .academicYear(e.getAcademicYear())
                .semester(e.getSemester())
                .enrollmentDate(e.getEnrollmentDate())
                .status(e.getStatus())
                .build();
    }
}
