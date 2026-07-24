package com.collegeerp.Backend.assignment.repository;

import com.collegeerp.Backend.assignment.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentSubmissionRepository
        extends JpaRepository<AssignmentSubmission, Long> {

    boolean existsByAssignmentIdAndStudentId(
            Long assignmentId,
            Long studentId);

    List<AssignmentSubmission> findByAssignmentId(Long assignmentId);

    List<AssignmentSubmission> findByStudentId(Long studentId);

    /**
     * Bulk variant of {@link #findByAssignmentId} so the teacher self-service assignments
     * endpoint can compute submitted/evaluated/pending counts for every owned assignment in
     * one query instead of one query per assignment.
     */
    List<AssignmentSubmission> findByAssignmentIdIn(List<Long> assignmentIds);

}