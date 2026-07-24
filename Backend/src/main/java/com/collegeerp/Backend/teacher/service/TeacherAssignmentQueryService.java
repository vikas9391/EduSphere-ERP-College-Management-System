package com.collegeerp.Backend.teacher.service;

import com.collegeerp.Backend.assignment.entity.Assignment;
import com.collegeerp.Backend.assignment.entity.AssignmentSubmission;
import com.collegeerp.Backend.assignment.repository.AssignmentRepository;
import com.collegeerp.Backend.assignment.repository.AssignmentSubmissionRepository;
import com.collegeerp.Backend.teacher.dto.TeacherAssignmentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read-only, teacher-scoped view over {@link Assignment}, with submission counts computed
 * server-side so the frontend no longer needs to download every submission in the system to
 * work out what's pending review (see README_PROGRESS.md, "Teacher/student dashboards still
 * over-fetch"). A submission counts as "evaluated" under the same convention
 * {@code StudentAttendanceService} uses for attendance status: a free-text column compared
 * case-insensitively, not an enum.
 */
@Service
@Transactional(readOnly = true)
public class TeacherAssignmentQueryService {

    private static final String EVALUATED_STATUS = "EVALUATED";

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;

    public TeacherAssignmentQueryService(AssignmentRepository assignmentRepository,
                                          AssignmentSubmissionRepository submissionRepository) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
    }

    public List<TeacherAssignmentResponse> getAssignments(Long teacherId) {

        List<Assignment> assignments = assignmentRepository.findByTeacherId(teacherId);
        if (assignments.isEmpty()) {
            return List.of();
        }

        List<Long> assignmentIds = assignments.stream().map(Assignment::getId).toList();

        Map<Long, List<AssignmentSubmission>> submissionsByAssignmentId =
                submissionRepository.findByAssignmentIdIn(assignmentIds)
                        .stream()
                        .collect(Collectors.groupingBy(s -> s.getAssignment().getId()));

        return assignments.stream()
                .map(a -> map(a, submissionsByAssignmentId.getOrDefault(a.getId(), List.of())))
                .toList();
    }

    private TeacherAssignmentResponse map(Assignment a, List<AssignmentSubmission> submissions) {
        long evaluated = submissions.stream()
                .filter(s -> EVALUATED_STATUS.equalsIgnoreCase(s.getStatus()))
                .count();

        return TeacherAssignmentResponse.builder()
                .assignmentId(a.getId())
                .title(a.getTitle())
                .subjectId(a.getSubject().getId())
                .subjectName(a.getSubject().getSubjectName())
                .dueDate(a.getDueDate())
                .maxMarks(a.getMaxMarks())
                .totalSubmissions(submissions.size())
                .evaluatedCount((int) evaluated)
                .pendingReviewCount((int) (submissions.size() - evaluated))
                .build();
    }
}
