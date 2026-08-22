package com.collegeerp.Backend.assignment.service;

import com.collegeerp.Backend.assignment.dto.AssignmentRequest;
import com.collegeerp.Backend.assignment.dto.AssignmentResponse;
import com.collegeerp.Backend.assignment.entity.Assignment;
import com.collegeerp.Backend.assignment.repository.AssignmentRepository;
import com.collegeerp.Backend.subject.entity.Subject;
import com.collegeerp.Backend.subject.repository.SubjectRepository;
import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.UserRepository;
import com.collegeerp.Backend.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            SubjectRepository subjectRepository,
            UserRepository userRepository) {
        this.assignmentRepository = assignmentRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
    }

    public AssignmentResponse createAssignment(
            AssignmentRequest request, UserPrincipal principal) {

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        validateAssignmentValues(request);
        requireTeacherCanManage(subject, teacher, principal);

        Assignment assignment = Assignment.builder()
                .subject(subject)
                .teacher(teacher)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .maxMarks(request.getMaxMarks())
                .createdAt(LocalDateTime.now())
                .build();

        return map(assignmentRepository.save(assignment));
    }

    public List<AssignmentResponse> getAllAssignments() {
        return assignmentRepository.findAll().stream().map(this::map).toList();
    }

    public AssignmentResponse getAssignment(Long id) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        return map(assignment);
    }

    public AssignmentResponse updateAssignment(
            Long id, AssignmentRequest request, UserPrincipal principal) {

        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        validateAssignmentValues(request);
        requireAssignmentOwner(assignment, principal);
        requireTeacherCanManage(subject, teacher, principal);

        assignment.setSubject(subject);
        assignment.setTeacher(teacher);
        assignment.setTitle(request.getTitle().trim());
        assignment.setDescription(request.getDescription());
        assignment.setDueDate(request.getDueDate());
        assignment.setMaxMarks(request.getMaxMarks());

        return map(assignmentRepository.save(assignment));
    }

    public void deleteAssignment(Long id, UserPrincipal principal) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        requireAssignmentOwner(assignment, principal);
        assignmentRepository.delete(assignment);
    }

    private void requireTeacherCanManage(
            Subject subject, User teacher, UserPrincipal principal) {

        if (isAdmin(principal)) {
            // Even admins cannot attach an assignment to a teacher who does not own the subject.
            if (!Objects.equals(subject.getTeacher().getId(), teacher.getId())) {
                throw new IllegalArgumentException("The selected teacher is not assigned to this subject");
            }
            return;
        }

        if (!"TEACHER".equalsIgnoreCase(principal.getRole())
                || !Objects.equals(principal.getId(), teacher.getId())
                || subject.getTeacher() == null
                || !Objects.equals(subject.getTeacher().getId(), principal.getId())) {
            throw new AccessDeniedException("You can manage assignments only for subjects assigned to you");
        }
    }

    private void requireAssignmentOwner(Assignment assignment, UserPrincipal principal) {
        if (isAdmin(principal)) {
            return;
        }
        if (!"TEACHER".equalsIgnoreCase(principal.getRole())
                || assignment.getTeacher() == null
                || !Objects.equals(assignment.getTeacher().getId(), principal.getId())) {
            throw new AccessDeniedException("You can modify only your own assignments");
        }
    }

    private boolean isAdmin(UserPrincipal principal) {
        return "ADMIN".equalsIgnoreCase(principal.getRole())
                || "SUPER_ADMIN".equalsIgnoreCase(principal.getRole());
    }

    private void validateAssignmentValues(AssignmentRequest request) {
        if (request.getSubjectId() == null || request.getTeacherId() == null) {
            throw new IllegalArgumentException("Subject and teacher are required");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Assignment title is required");
        }
        if (request.getDueDate() == null) {
            throw new IllegalArgumentException("Assignment due date is required");
        }
        if (request.getMaxMarks() == null || request.getMaxMarks() <= 0) {
            throw new IllegalArgumentException("Maximum marks must be greater than zero");
        }
    }

    private AssignmentResponse map(Assignment a) {
        return AssignmentResponse.builder()
                .id(a.getId())
                .subjectId(a.getSubject().getId())
                .subjectName(a.getSubject().getSubjectName())
                .teacherId(a.getTeacher().getId())
                .teacherName(a.getTeacher().getFirstName() + " " + a.getTeacher().getLastName())
                .title(a.getTitle())
                .description(a.getDescription())
                .dueDate(a.getDueDate())
                .maxMarks(a.getMaxMarks())
                .build();
    }
}
