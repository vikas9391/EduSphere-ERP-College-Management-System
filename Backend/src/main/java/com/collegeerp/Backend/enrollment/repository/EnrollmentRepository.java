package com.collegeerp.Backend.enrollment.repository;

import com.collegeerp.Backend.enrollment.entity.Enrollment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndSubjectId(Long studentId, Long subjectId);

    List<Enrollment> findByStudentId(Long studentId);

    /**
     * Fetch-joins subject -> course -> department and subject -> teacher, so the student
     * self-service endpoints (dashboard, enrollments, subjects) can render everything in one
     * query without N+1 lazy-loading. {@code course.department} uses LEFT JOIN FETCH because
     * {@code Course.department} is nullable.
     */
    @Query("""
           SELECT e
           FROM Enrollment e
           JOIN FETCH e.subject s
           JOIN FETCH s.course c
           LEFT JOIN FETCH c.department
           JOIN FETCH s.teacher
           WHERE e.student.id = :studentId
           ORDER BY e.enrollmentDate DESC
           """)
    List<Enrollment> findByStudentIdWithDetails(Long studentId);

    /**
     * Fetch-joins student + subject -> course -> department, so the teacher self-service
     * roster endpoint ("my students") can render every enrolled student across every
     * subject this teacher owns without N+1 lazy-loading. One row per (student, subject)
     * pair, same shape as {@link #findByStudentIdWithDetails} - a teacher with the same
     * student in two subjects sees two rows, which is correct: it's a roster of
     * enrollments, not a deduplicated student list.
     */
    @Query("""
           SELECT e
           FROM Enrollment e
           JOIN FETCH e.student
           JOIN FETCH e.subject s
           JOIN FETCH s.course c
           LEFT JOIN FETCH c.department
           JOIN FETCH s.teacher
           WHERE s.teacher.id = :teacherId
           ORDER BY s.subjectCode ASC, e.student.rollNumber ASC
           """)
    List<Enrollment> findBySubjectTeacherIdWithDetails(Long teacherId);
}