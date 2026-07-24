package com.collegeerp.Backend.examination.repository;

import com.collegeerp.Backend.examination.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    @Query("""
           SELECT e
           FROM Exam e
           JOIN FETCH e.course
           """)
    List<Exam> findAllWithCourse();

    @Query("""
           SELECT e
           FROM Exam e
           JOIN FETCH e.course
           WHERE e.id = :id
           """)
    Optional<Exam> findByIdWithCourse(Long id);

    List<Exam> findBySemesterAndAcademicYear(Integer semester, String academicYear);
}
