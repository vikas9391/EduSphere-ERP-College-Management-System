package com.collegeerp.Backend.marks.repository;

import com.collegeerp.Backend.marks.entity.Marks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MarksRepository extends JpaRepository<Marks, Long> {

    boolean existsByExamScheduleIdAndClassEnrollmentId(Long examScheduleId, Long classEnrollmentId);

    @Query("""
           SELECT m
           FROM Marks m
           JOIN FETCH m.examSchedule es
           JOIN FETCH es.exam ex
           JOIN FETCH es.classSubject cs
           LEFT JOIN FETCH cs.subject
           JOIN FETCH m.classEnrollment ce
           JOIN FETCH ce.student
           WHERE es.id = :examScheduleId
           """)
    List<Marks> findByExamScheduleIdWithDetails(Long examScheduleId);

    @Query("""
           SELECT m
           FROM Marks m
           JOIN FETCH m.examSchedule es
           JOIN FETCH es.exam ex
           JOIN FETCH es.classSubject cs
           LEFT JOIN FETCH cs.subject
           JOIN FETCH m.classEnrollment ce
           JOIN FETCH ce.student
           WHERE ce.student.id = :studentId
             AND ex.semester = :semester
             AND ex.academicYear = :academicYear
             AND m.status = 'PUBLISHED'
           """)
    List<Marks> findPublishedByStudentAndSemester(Long studentId, Integer semester, String academicYear);

    @Query("""
           SELECT m
           FROM Marks m
           JOIN FETCH m.examSchedule es
           JOIN FETCH es.exam ex
           JOIN FETCH es.classSubject cs
           LEFT JOIN FETCH cs.subject
           JOIN FETCH m.classEnrollment ce
           JOIN FETCH ce.student
           WHERE ce.student.id = :studentId
             AND m.status = 'PUBLISHED'
           """)
    List<Marks> findAllPublishedByStudent(Long studentId);

    @Query("""
           SELECT m
           FROM Marks m
           JOIN FETCH m.examSchedule es
           JOIN FETCH es.exam ex
           JOIN FETCH es.classSubject cs
           LEFT JOIN FETCH cs.subject
           JOIN FETCH m.classEnrollment ce
           JOIN FETCH ce.student
           WHERE m.id = :id
           """)
    Optional<Marks> findByIdWithDetails(Long id);
}
