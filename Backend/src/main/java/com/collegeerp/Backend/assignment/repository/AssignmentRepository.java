package com.collegeerp.Backend.assignment.repository;

import com.collegeerp.Backend.assignment.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AssignmentRepository
        extends JpaRepository<Assignment, Long> {

    /**
     * Fetch-joins subject + teacher so the student self-service assignments endpoint can
     * render everything (subject name, teacher name) without N+1 lazy-loading. Used to fetch
     * all assignments across every subject a student is enrolled in.
     */
    @Query("""
           SELECT a
           FROM Assignment a
           JOIN FETCH a.subject
           JOIN FETCH a.teacher
           WHERE a.subject.id IN :subjectIds
           ORDER BY a.dueDate ASC
           """)
    List<Assignment> findBySubjectIdIn(List<Long> subjectIds);

    /**
     * Fetch-joins subject so the teacher self-service assignments endpoint can render
     * subject name without N+1 lazy-loading (teacher is already known - it's the filter).
     * Mirrors {@link #findBySubjectIdIn}.
     */
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
