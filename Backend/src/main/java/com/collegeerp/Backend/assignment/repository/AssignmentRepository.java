package com.collegeerp.Backend.assignment.repository;

import com.collegeerp.Backend.assignment.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    /**
     * Returns assignments applicable to the student's class subjects. Null class_subject_id
     * rows are legacy assignments and remain visible for compatibility during migration.
     */
    @Query("""
           SELECT DISTINCT a
           FROM Assignment a
           JOIN FETCH a.subject
           JOIN FETCH a.teacher
           WHERE (a.classSubject.id IN :classSubjectIds)
              OR (a.classSubject IS NULL AND a.subject.id IN :subjectIds)
           ORDER BY a.dueDate ASC
           """)
    List<Assignment> findForStudentClassSubjects(List<Long> classSubjectIds, List<Long> subjectIds);

    @Query("""
           SELECT a
           FROM Assignment a
           JOIN FETCH a.subject
           JOIN FETCH a.teacher
           WHERE a.subject.id IN :subjectIds
           ORDER BY a.dueDate ASC
           """)
    List<Assignment> findBySubjectIdIn(List<Long> subjectIds);

    @Query("""
           SELECT a
           FROM Assignment a
           JOIN FETCH a.subject
           JOIN FETCH a.teacher
           WHERE a.teacher.id = :teacherId
           ORDER BY a.dueDate DESC
           """)
    List<Assignment> findByTeacherId(Long teacherId);
}