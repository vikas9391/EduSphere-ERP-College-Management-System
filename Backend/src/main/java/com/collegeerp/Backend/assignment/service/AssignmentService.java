package com.collegeerp.Backend.assignment.service;

import com.collegeerp.Backend.assignment.dto.AssignmentRequest;
import com.collegeerp.Backend.assignment.dto.AssignmentResponse;
import com.collegeerp.Backend.assignment.entity.Assignment;
import com.collegeerp.Backend.assignment.repository.AssignmentRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.repository.ClassSubjectRepository;
import com.collegeerp.Backend.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ClassSubjectRepository classSubjectRepository;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             ClassSubjectRepository classSubjectRepository) {
        this.assignmentRepository = assignmentRepository;
        this.classSubjectRepository = classSubjectRepository;
    }

    public AssignmentResponse createAssignment(AssignmentRequest request, UserPrincipal principal) {
        validateAssignmentValues(request);
        ClassSubject classSubject = resolveClassSubject(request.getClassSubjectId());
        requireClassSubjectTeacher(classSubject, principal);

        Assignment assignment = Assignment.builder()
                .classSubject(classSubject)
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
            return assignmentRepository.findAllWithDetails().stream().map(this::map).toList();
        }
        requireTeacher(principal);
        return assignmentRepository.findByTeacherId(principal.getId()).stream().map(this::map).toList();
    }

    public AssignmentResponse getAssignment(Long id, UserPrincipal principal) {
        Assignment assignment = assignmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        requireAssignmentOwner(assignment, principal);
        return map(assignment);
    }

    public AssignmentResponse updateAssignment(Long id, AssignmentRequest request, UserPrincipal principal) {
        Assignment assignment = assignmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        validateAssignmentValues(request);
        requireAssignmentOwner(assignment, principal);

        ClassSubject classSubject = resolveClassSubject(request.getClassSubjectId());
        requireClassSubjectTeacher(classSubject, principal);

        assignment.setClassSubject(classSubject);
        assignment.setTitle(request.getTitle().trim());
        assignment.setDescription(request.getDescription());
        assignment.setDueDate(request.getDueDate());
        assignment.setMaxMarks(request.getMaxMarks());

        return map(assignmentRepository.save(assignment));
    }

    public void deleteAssignment(Long id, UserPrincipal principal) {
        Assignment assignment = assignmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        requireAssignmentOwner(assignment, principal);
        assignmentRepository.delete(assignment);
    }

    private ClassSubject resolveClassSubject(Long classSubjectId) {
        return classSubjectRepository.findByIdWithRelations(classSubjectId)
                .orElseThrow(() -> new RuntimeException("Class subject not found"));
    }

    private void requireClassSubjectTeacher(ClassSubject classSubject, UserPrincipal principal) {
        if (isAdmin(principal)) {
            return;
        }
        requireTeacher(principal);
        if (classSubject.getTeacher() == null
                || !Objects.equals(classSubject.getTeacher().getId(), principal.getId())) {
            throw new AccessDeniedException("You can manage assignments only for class subjects assigned to you");
        }
    }

    private void requireAssignmentOwner(Assignment assignment, UserPrincipal principal) {
        if (isAdmin(principal)) {
            return;
        }
        requireTeacher(principal);
        if (assignment.getClassSubject() == null
                || assignment.getClassSubject().getTeacher() == null
                || !Objects.equals(assignment.getClassSubject().getTeacher().getId(), principal.getId())) {
            throw new AccessDeniedException("You can access only assignments for your class subjects");
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
        if (request.getClassSubjectId() == null) {
            throw new IllegalArgumentException("Class subject is required");
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
        ClassSubject cs = a.getClassSubject();
        var schoolClass = cs.getSchoolClass();
        var teacher = cs.getTeacher();
        var linkedSubject = cs.getSubject();
        return AssignmentResponse.builder()
                .id(a.getId())
                .subjectId(linkedSubject != null ? linkedSubject.getId() : null)
                .subjectName(linkedSubject != null ? linkedSubject.getSubjectName() : cs.getSubjectName())
                .classSubjectId(cs.getId())
                .classId(schoolClass != null ? schoolClass.getId() : null)
                .className(schoolClass != null ? schoolClass.getName() : null)
                .teacherId(teacher != null ? teacher.getId() : null)
                .teacherName(teacher != null ? teacher.getFirstName() + " " + teacher.getLastName() : null)
                .title(a.getTitle())
                .description(a.getDescription())
                .dueDate(a.getDueDate())
                .maxMarks(a.getMaxMarks())
                .build();
    }
}
