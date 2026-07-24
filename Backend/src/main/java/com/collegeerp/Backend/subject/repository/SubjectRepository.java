package com.collegeerp.Backend.subject.repository;

import com.collegeerp.Backend.subject.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsBySubjectCode(String subjectCode);

    /**
     * Fetch-joins {@code course} and {@code teacher} to avoid N+1 queries when mapping to
     * {@code SubjectResponse}. Explicit {@code countQuery} required for the same reason as
     * {@code CourseRepository.findAllWithDepartment} - Hibernate can't safely auto-derive a
     * count from a query containing JOIN FETCH.
     */
    @Query(
        value = """
                SELECT s
                FROM Subject s
                JOIN FETCH s.course
                JOIN FETCH s.teacher
                """,
        countQuery = "SELECT count(s) FROM Subject s"
    )
    Page<Subject> findAllWithRelations(Pageable pageable);

    @Query("""
            SELECT s
            FROM Subject s
            JOIN FETCH s.course
            JOIN FETCH s.teacher
            WHERE s.id=:id
            """)
    Optional<Subject> findByIdWithRelations(Long id);

    /**
     * Fetch-joins {@code course} (teacher is already known - it's the filter) so the
     * teacher self-service "my subjects" endpoint can render course names without N+1
     * lazy-loading. Mirrors {@code EnrollmentRepository.findByStudentIdWithDetails}.
     */
    @Query("""
            SELECT s
            FROM Subject s
            JOIN FETCH s.course
            JOIN FETCH s.teacher
            WHERE s.teacher.id = :teacherId
            ORDER BY s.subjectCode ASC
            """)
    java.util.List<Subject> findByTeacherIdWithRelations(Long teacherId);
}
