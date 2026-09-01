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
           JOIN FETCH a.subject
           JOIN FETCH a.teacher
           LEFT JOIN FETCH a.classSubject cs
           LEFT JOIN FETCH cs.schoolClass
           WHERE (cs.id IN :classSubjectIds)
              OR (cs IS NULL AND a.subject.id IN :subjectIds)
           ORDER BY a.dueDate ASC
           """)
    List<Assignment> findForStudentClassSubjects(List<Long> classSubjectIds, List<Long> subjectIds);

    @Query("""
           SELECT a
           FROM Assignment a
           JOIN FETCH a.subject
           JOIN FETCH a.teacher
           LEFT JOIN FETCH a.classSubject cs
           LEFT JOIN FETCH cs.schoolClass
           WHERE a.subject.id IN :subjectIds
           ORDER BY a.dueDate ASC
           """)
    List<Assignment> findBySubjectIdIn(List<Long> subjectIds);

    @Query("""
           SELECT a
           FROM Assignment a
           JOIN FETCH a.subject
           JOIN FETCH a.teacher
           LEFT JOIN FETCH a.classSubject cs
           LEFT JOIN FETCH cs.schoolClass
           WHERE a.teacher.id = :teacherId
           ORDER BY a.dueDate DESC
           """)
    List<Assignment> findByTeacherId(Long teacherId);

    @Query("""
           SELECT a
           FROM Assignment a
           JOIN FETCH a.subject
           JOIN FETCH a.teacher
           LEFT JOIN FETCH a.classSubject cs
           LEFT JOIN FETCH cs.schoolClass
           ORDER BY a.dueDate DESC
           """)
    List<Assignment> findAllWithDetails();

    @Query("""
           SELECT a
           FROM Assignment a
           JOIN FETCH a.subject
           JOIN FETCH a.teacher
           LEFT JOIN FETCH a.classSubject cs
           LEFT JOIN FETCH cs.schoolClass
           WHERE a.id = :id
           """)
    Optional<Assignment> findByIdWithDetails(Long id);
}
