package com.collegeerp.Backend.assignment.service;

import com.collegeerp.Backend.assignment.dto.AssignmentRequest;
import com.collegeerp.Backend.assignment.dto.AssignmentResponse;
import com.collegeerp.Backend.assignment.entity.Assignment;
import com.collegeerp.Backend.assignment.repository.AssignmentRepository;
import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.UserRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.repository.ClassSubjectRepository;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.subject.entity.Subject;
import com.collegeerp.Backend.subject.repository.SubjectRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final UserRepository userRepository;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            SubjectRepository subjectRepository,
            ClassSubjectRepository classSubjectRepository,
            UserRepository userRepository) {
        this.assignmentRepository = assignmentRepository;
        this.subjectRepository = subjectRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.userRepository = userRepository;
    }

    public AssignmentResponse createAssignment(AssignmentRequest request, UserPrincipal principal) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        validateAssignmentValues(request);
        ClassSubject classSubject = resolveClassSubject(request);
        requireTeacherCanManage(subject, teacher, classSubject, principal);

        Assignment assignment = Assignment.builder()
                .subject(subject)
                .classSubject(classSubject)
                .teacher(teacher)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .maxMarks(request.getMaxMarks())
                .createdAt(LocalDateTime.now())
                .build();

        return map(assignmentRepository.save(assignment));
    }

    public List<AssignmentResponse> getAllAssignments(UserPrincipal principal) {
        if (isAdmin(principal)) {
            return assignmentRepository.findAll().stream().map(this::map).toList();
        }
        requireTeacher(principal);
        return assignmentRepository.findByTeacherId(principal.getId()).stream().map(this::map).toList();
    }

    public AssignmentResponse getAssignment(Long id, UserPrincipal principal) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        requireAssignmentOwner(assignment, principal);
        return map(assignment);
    }

    public AssignmentResponse updateAssignment(Long id, AssignmentRequest request, UserPrincipal principal) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        validateAssignmentValues(request);
        ClassSubject classSubject = resolveClassSubject(request);
        requireAssignmentOwner(assignment, principal);
        requireTeacherCanManage(subject, teacher, classSubject, principal);

        assignment.setSubject(subject);
        assignment.setClassSubject(classSubject);
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

    private ClassSubject resolveClassSubject(AssignmentRequest request) {
        if (request.getClassSubjectId() == null) {
            return null;
        }
        ClassSubject classSubject = classSubjectRepository.findByIdWithRelations(request.getClassSubjectId())
                .orElseThrow(() -> new RuntimeException("Class subject not found"));
        if (classSubject.getSubject() != null
                && !Objects.equals(classSubject.getSubject().getId(), request.getSubjectId())) {
            throw new IllegalArgumentException("Class subject does not belong to the selected subject");
        }
        return classSubject;
    }

    private void requireTeacherCanManage(
            Subject subject, User teacher, ClassSubject classSubject, UserPrincipal principal) {

        if (classSubject != null
                && classSubject.getTeacher() != null
                && !Objects.equals(classSubject.getTeacher().getId(), teacher.getId())) {
            throw new IllegalArgumentException("The selected teacher does not teach this class subject");
        }

        if (isAdmin(principal)) {
            if (subject.getTeacher() != null && !Objects.equals(subject.getTeacher().getId(), teacher.getId())
                    && classSubject == null) {
                throw new IllegalArgumentException("The selected teacher is not assigned to this subject");
            }
            return;
        }

        requireTeacher(principal);
        if (!Objects.equals(principal.getId(), teacher.getId())
                || (classSubject != null
                    ? classSubject.getTeacher() == null || !Objects.equals(classSubject.getTeacher().getId(), principal.getId())
                    : subject.getTeacher() == null || !Objects.equals(subject.getTeacher().getId(), principal.getId()))) {
            throw new AccessDeniedException("You can manage assignments only for subjects assigned to you");
        }
    }

    private void requireAssignmentOwner(Assignment assignment, UserPrincipal principal) {
        if (isAdmin(principal)) {
            return;
        }
        requireTeacher(principal);
        if (assignment.getTeacher() == null
                || !Objects.equals(assignment.getTeacher().getId(), principal.getId())) {
            throw new AccessDeniedException("You can access only your own assignments");
        }
    }

    private void requireTeacher(UserPrincipal principal) {
        if (!"TEACHER".equalsIgnoreCase(principal.getRole())) {
            throw new AccessDeniedException("Only teachers or admins can access assignment administration");
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
