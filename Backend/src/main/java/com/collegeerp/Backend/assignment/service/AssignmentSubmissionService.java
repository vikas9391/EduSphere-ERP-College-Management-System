package com.collegeerp.Backend.assignment.service;

import com.collegeerp.Backend.assignment.dto.AssignmentSubmissionRequest;
import com.collegeerp.Backend.assignment.dto.AssignmentSubmissionResponse;
import com.collegeerp.Backend.assignment.entity.Assignment;
import com.collegeerp.Backend.assignment.entity.AssignmentSubmission;
import com.collegeerp.Backend.assignment.repository.AssignmentRepository;
import com.collegeerp.Backend.assignment.repository.AssignmentSubmissionRepository;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AssignmentSubmissionService {

    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final StudentRepository studentRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    public AssignmentSubmissionService(
            AssignmentSubmissionRepository submissionRepository,
            AssignmentRepository assignmentRepository,
            StudentRepository studentRepository,
            ClassEnrollmentRepository classEnrollmentRepository) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
    }

    public AssignmentSubmissionResponse submitAssignment(AssignmentSubmissionRequest request) {
        Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (submissionRepository.existsByAssignmentIdAndStudentId(assignment.getId(), student.getId())) {
            throw new RuntimeException("Assignment already submitted.");
        }

        requireEligibleStudent(assignment, student.getId());

        if (request.getSubmissionUrl() == null || request.getSubmissionUrl().isBlank()) {
            throw new IllegalArgumentException("Submission URL is required");
        }

        AssignmentSubmission submission = AssignmentSubmission.builder()
                .assignment(assignment)
                .student(student)
                .submissionUrl(request.getSubmissionUrl().trim())
                .submittedAt(LocalDateTime.now())
                .status("SUBMITTED")
                .build();

        return map(submissionRepository.save(submission));
    }

    public List<AssignmentSubmissionResponse> getAllSubmissions(UserPrincipal principal) {
        if (isAdmin(principal)) {
            return submissionRepository.findAll().stream().map(this::map).toList();
        }
        requireTeacher(principal);
        return submissionRepository.findByAssignmentTeacherId(principal.getId())
                .stream().map(this::map).toList();
    }

    public List<AssignmentSubmissionResponse> getAssignmentSubmissions(
            Long assignmentId, UserPrincipal principal) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        requireAssignmentOwner(assignment, principal);
        return submissionRepository.findByAssignmentIdWithDetails(assignmentId)
                .stream().map(this::map).toList();
    }

    public AssignmentSubmissionResponse evaluateSubmission(
            Long id, Integer marks, String feedback, UserPrincipal principal) {
        AssignmentSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        requireAssignmentOwner(submission.getAssignment(), principal);

        Integer maxMarks = submission.getAssignment().getMaxMarks();
        if (marks == null || marks < 0 || marks > maxMarks) {
            throw new IllegalArgumentException("Marks must be between 0 and " + maxMarks);
        }

        submission.setMarks(marks);
        submission.setFeedback(feedback);
        submission.setStatus("EVALUATED");

        return map(submissionRepository.save(submission));
    }

    private void requireEligibleStudent(Assignment assignment, Long studentId) {
        if (assignment.getClassSubject() == null) {
            throw new AccessDeniedException(
                    "This assignment is not class-scoped and cannot accept new submissions");
        }

        if (!classEnrollmentRepository.existsByClassSubjectIdAndStudentId(
                assignment.getClassSubject().getId(), studentId)) {
            throw new AccessDeniedException(
                    "You are not enrolled in the class subject for this assignment");
        }
    }

    private void requireAssignmentOwner(Assignment assignment, UserPrincipal principal) {
        if (isAdmin(principal)) {
            return;
        }
        requireTeacher(principal);
        if (assignment.getTeacher() == null
                || !Objects.equals(assignment.getTeacher().getId(), principal.getId())) {
            throw new AccessDeniedException("You can manage only submissions for your own assignments");
        }
    }

    private void requireTeacher(UserPrincipal principal) {
        if (!"TEACHER".equalsIgnoreCase(principal.getRole())) {
            throw new AccessDeniedException("Only teachers can access teacher-scoped submissions");
        }
    }

    private boolean isAdmin(UserPrincipal principal) {
        return "ADMIN".equalsIgnoreCase(principal.getRole())
                || "SUPER_ADMIN".equalsIgnoreCase(principal.getRole());
    }

    private AssignmentSubmissionResponse map(AssignmentSubmission s) {
        return AssignmentSubmissionResponse.builder()
                .id(s.getId())
                .assignmentId(s.getAssignment().getId())
                .assignmentTitle(s.getAssignment().getTitle())
                .studentId(s.getStudent().getId())
                .studentName(s.getStudent().getFirstName() + " " + s.getStudent().getLastName())
                .submissionUrl(s.getSubmissionUrl())
                .submittedAt(s.getSubmittedAt())
                .marks(s.getMarks())
                .feedback(s.getFeedback())
                .status(s.getStatus())
                .build();
    }
}
