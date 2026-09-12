package com.collegeerp.Backend.assignment.repository;

import com.collegeerp.Backend.assignment.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @Query("""
           SELECT DISTINCT a
           FROM Assignment a
           JOIN FETCH a.classSubject cs
           LEFT JOIN FETCH cs.subject
           LEFT JOIN FETCH cs.schoolClass
           LEFT JOIN FETCH cs.teacher
           WHERE cs.id IN :classSubjectIds
           ORDER BY a.dueDate ASC
           """)
    List<Assignment> findForStudentClassSubjects(List<Long> classSubjectIds);

    @Query("""
           SELECT a
           FROM Assignment a
           JOIN FETCH a.classSubject cs
           LEFT JOIN FETCH cs.subject
           LEFT JOIN FETCH cs.schoolClass
           LEFT JOIN FETCH cs.teacher t
           WHERE t.id = :teacherId
           ORDER BY a.dueDate DESC
           """)
    List<Assignment> findByTeacherId(Long teacherId);

    @Query("""
           SELECT a
           FROM Assignment a
           JOIN FETCH a.classSubject cs
           LEFT JOIN FETCH cs.subject
           LEFT JOIN FETCH cs.schoolClass
           LEFT JOIN FETCH cs.teacher
           ORDER BY a.dueDate DESC
           """)
    List<Assignment> findAllWithDetails();

    @Query("""
           SELECT a
           FROM Assignment a
           JOIN FETCH a.classSubject cs
           LEFT JOIN FETCH cs.subject
           LEFT JOIN FETCH cs.schoolClass
           LEFT JOIN FETCH cs.teacher
           WHERE a.id = :id
           """)
    Optional<Assignment> findByIdWithDetails(Long id);
}
