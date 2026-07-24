package com.collegeerp.Backend.assignment.service;

import com.collegeerp.Backend.assignment.dto.AssignmentSubmissionRequest;
import com.collegeerp.Backend.assignment.dto.AssignmentSubmissionResponse;
import com.collegeerp.Backend.assignment.entity.Assignment;
import com.collegeerp.Backend.assignment.entity.AssignmentSubmission;
import com.collegeerp.Backend.assignment.repository.AssignmentRepository;
import com.collegeerp.Backend.assignment.repository.AssignmentSubmissionRepository;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssignmentSubmissionService {

    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final StudentRepository studentRepository;

    public AssignmentSubmissionService(
            AssignmentSubmissionRepository submissionRepository,
            AssignmentRepository assignmentRepository,
            StudentRepository studentRepository) {

        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.studentRepository = studentRepository;
    }

    public AssignmentSubmissionResponse submitAssignment(
            AssignmentSubmissionRequest request) {

        if (submissionRepository.existsByAssignmentIdAndStudentId(
                request.getAssignmentId(),
                request.getStudentId())) {

            throw new RuntimeException("Assignment already submitted.");
        }

        Assignment assignment = assignmentRepository.findById(
                request.getAssignmentId())
                .orElseThrow(() ->
                        new RuntimeException("Assignment not found"));

        Student student = studentRepository.findById(
                request.getStudentId())
                .orElseThrow(() ->
                        new RuntimeException("Student not found"));

        AssignmentSubmission submission =
                AssignmentSubmission.builder()
                        .assignment(assignment)
                        .student(student)
                        .submissionUrl(request.getSubmissionUrl())
                        .submittedAt(LocalDateTime.now())
                        .status("SUBMITTED")
                        .build();

        submission = submissionRepository.save(submission);

        return map(submission);
    }

    public List<AssignmentSubmissionResponse> getAllSubmissions() {

        return submissionRepository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    public List<AssignmentSubmissionResponse> getAssignmentSubmissions(
            Long assignmentId) {

        return submissionRepository.findByAssignmentId(assignmentId)
                .stream()
                .map(this::map)
                .toList();
    }

    public AssignmentSubmissionResponse evaluateSubmission(
            Long id,
            Integer marks,
            String feedback) {

        AssignmentSubmission submission =
                submissionRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Submission not found"));

        submission.setMarks(marks);
        submission.setFeedback(feedback);
        submission.setStatus("EVALUATED");

        submission = submissionRepository.save(submission);

        return map(submission);
    }

    private AssignmentSubmissionResponse map(
            AssignmentSubmission s) {

        return AssignmentSubmissionResponse.builder()
                .id(s.getId())
                .assignmentId(s.getAssignment().getId())
                .assignmentTitle(s.getAssignment().getTitle())
                .studentId(s.getStudent().getId())
                .studentName(
                        s.getStudent().getFirstName()
                                + " "
                                + s.getStudent().getLastName())
                .submissionUrl(s.getSubmissionUrl())
                .submittedAt(s.getSubmittedAt())
                .marks(s.getMarks())
                .feedback(s.getFeedback())
                .status(s.getStatus())
                .build();
    }

}