package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.enrollment.dto.EnrollmentResponse;
import com.collegeerp.Backend.enrollment.entity.Enrollment;
import com.collegeerp.Backend.enrollment.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only, student-scoped view over {@link Enrollment}. Deliberately reuses
 * {@link EnrollmentResponse} (the same DTO the admin {@code EnrollmentController} returns)
 * rather than inventing a parallel DTO, since the shape a student needs to see their own
 * enrollment is identical to what an admin sees for one row.
 */
@Service
@Transactional(readOnly = true)
public class StudentEnrollmentQueryService {

    private final EnrollmentRepository enrollmentRepository;

    public StudentEnrollmentQueryService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<EnrollmentResponse> getEnrollments(Long studentId) {
        return enrollmentRepository.findByStudentIdWithDetails(studentId)
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
