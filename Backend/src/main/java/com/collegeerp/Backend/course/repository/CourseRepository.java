package com.collegeerp.Backend.course.repository;

import com.collegeerp.Backend.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsByCourseCode(String courseCode);

    /**
     * Fetch-joins {@code department} to avoid N+1 queries when mapping to
     * {@code CourseResponse} (which needs department id/name for every row).
     * A separate {@code countQuery} is required because Hibernate can't count
     * a fetch-join query directly (the join would incorrectly affect the count
     * for *-to-many relations; harmless here since department is *-to-one, but
     * the explicit countQuery is still required syntactically and is cheaper anyway).
     */
    @Query(
        value = """
               SELECT c
               FROM Course c
               JOIN FETCH c.department
               """,
        countQuery = "SELECT count(c) FROM Course c"
    )
    Page<Course> findAllWithDepartment(Pageable pageable);

    @Query("""
           SELECT c
           FROM Course c
           JOIN FETCH c.department
           WHERE c.id = :id
           """)
    Optional<Course> findByIdWithDepartment(Long id);
}
