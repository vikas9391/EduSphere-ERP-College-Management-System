package com.collegeerp.Backend.student.repository;

import com.collegeerp.Backend.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByAdmissionNo(String admissionNo);

    boolean existsByEmail(String email);

    boolean existsByRollNumber(String rollNumber);

    Optional<Student> findByEmail(String email);

    /**
     * Fetch-joins {@code course} to avoid N+1 queries when mapping to
     * {@code StudentResponse} (which needs courseId/courseName for every row). LEFT
     * JOIN (not INNER) because {@code course} is nullable - a student with no course
     * assigned yet must still be returned. Separate {@code countQuery} for the same
     * reason as {@code CourseRepository#findAllWithDepartment}: Hibernate can't count a
     * fetch-join query directly.
     */
    @Query(
        value = "SELECT s FROM Student s LEFT JOIN FETCH s.course",
        countQuery = "SELECT count(s) FROM Student s"
    )
    Page<Student> findAllWithCourse(Pageable pageable);

    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.course WHERE s.id = :id")
    Optional<Student> findByIdWithCourse(Long id);
}