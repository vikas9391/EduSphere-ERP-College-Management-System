package com.collegeerp.Backend.assignment.repository;

import com.collegeerp.Backend.assignment.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AssignmentSubmissionRepository
        extends JpaRepository<AssignmentSubmission, Long> {

    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    List<AssignmentSubmission> findByAssignmentId(Long assignmentId);

    List<AssignmentSubmission> findByStudentId(Long studentId);

    /** Bulk variant used by teacher assignment dashboards. */
    List<AssignmentSubmission> findByAssignmentIdIn(List<Long> assignmentIds);

    @Query("""
           SELECT s
           FROM AssignmentSubmission s
           JOIN FETCH s.assignment a
           JOIN FETCH s.student
           WHERE a.teacher.id = :teacherId
           ORDER BY s.submittedAt DESC
           """)
    List<AssignmentSubmission> findByAssignmentTeacherId(Long teacherId);

    @Query("""
           SELECT s
           FROM AssignmentSubmission s
           JOIN FETCH s.assignment a
           JOIN FETCH s.student
           WHERE s.assignment.id = :assignmentId
           ORDER BY s.submittedAt DESC
           """)
    List<AssignmentSubmission> findByAssignmentIdWithDetails(Long assignmentId);
}
