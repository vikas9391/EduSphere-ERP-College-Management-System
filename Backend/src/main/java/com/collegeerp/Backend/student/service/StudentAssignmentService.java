package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.assignment.entity.Assignment;
import com.collegeerp.Backend.assignment.entity.AssignmentSubmission;
import com.collegeerp.Backend.assignment.repository.AssignmentRepository;
import com.collegeerp.Backend.assignment.repository.AssignmentSubmissionRepository;
import com.collegeerp.Backend.enrollment.repository.EnrollmentRepository;
import com.collegeerp.Backend.student.dto.StudentAssignmentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@Transactional(readOnly = true)
public class StudentAssignmentService {

    private static final String NOT_SUBMITTED = "NOT_SUBMITTED";

    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;

    public StudentAssignmentService(EnrollmentRepository enrollmentRepository,
                                     AssignmentRepository assignmentRepository,
                                     AssignmentSubmissionRepository submissionRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
    }

    public List<StudentAssignmentResponse> getAssignments(Long studentId) {

        List<Long> subjectIds = enrollmentRepository.findByStudentIdWithDetails(studentId)
                .stream()
                .map(e -> e.getSubject().getId())
                .distinct()
                .toList();

        if (subjectIds.isEmpty()) {
            return List.of();
        }

        List<Assignment> assignments = assignmentRepository.findBySubjectIdIn(subjectIds);

        Map<Long, AssignmentSubmission> submissionsByAssignmentId = submissionRepository.findByStudentId(studentId)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        s -> s.getAssignment().getId(),
                        Function.identity()
                ));

        return assignments.stream()
                .map(a -> map(a, submissionsByAssignmentId.get(a.getId())))
                .toList();
    }

    private StudentAssignmentResponse map(Assignment assignment, AssignmentSubmission submission) {

        var builder = StudentAssignmentResponse.builder()
                .assignmentId(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .subjectId(assignment.getSubject().getId())
                .subjectName(assignment.getSubject().getSubjectName())
                .teacherName(assignment.getTeacher().getFirstName() + " " + assignment.getTeacher().getLastName())
                .dueDate(assignment.getDueDate())
                .maxMarks(assignment.getMaxMarks());

        if (submission == null) {
            return builder.submissionStatus(NOT_SUBMITTED).build();
        }

        return builder
                .submissionStatus(submission.getStatus() != null ? submission.getStatus() : "SUBMITTED")
                .submittedAt(submission.getSubmittedAt())
                .marksObtained(submission.getMarks())
                .feedback(submission.getFeedback())
                .build();
    }
}
