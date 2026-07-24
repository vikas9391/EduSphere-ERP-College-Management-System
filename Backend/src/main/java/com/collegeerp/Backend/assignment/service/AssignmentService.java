package com.collegeerp.Backend.assignment.service;

import com.collegeerp.Backend.assignment.dto.AssignmentRequest;
import com.collegeerp.Backend.assignment.dto.AssignmentResponse;
import com.collegeerp.Backend.assignment.entity.Assignment;
import com.collegeerp.Backend.assignment.repository.AssignmentRepository;
import com.collegeerp.Backend.subject.entity.Subject;
import com.collegeerp.Backend.subject.repository.SubjectRepository;
import com.collegeerp.Backend.teacher.entity.Teacher;
import com.collegeerp.Backend.teacher.repository.TeacherRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            SubjectRepository subjectRepository,
            TeacherRepository teacherRepository) {

        this.assignmentRepository = assignmentRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
    }

    public AssignmentResponse createAssignment(
            AssignmentRequest request) {

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() ->
                        new RuntimeException("Subject not found"));

        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() ->
                        new RuntimeException("Teacher not found"));

        Assignment assignment = Assignment.builder()
                .subject(subject)
                .teacher(teacher)
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .maxMarks(request.getMaxMarks())
                .createdAt(LocalDateTime.now())
                .build();

        assignment = assignmentRepository.save(assignment);

        return map(assignment);
    }

    public List<AssignmentResponse> getAllAssignments() {

        return assignmentRepository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    public AssignmentResponse getAssignment(Long id) {

        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Assignment not found"));

        return map(assignment);
    }

    public AssignmentResponse updateAssignment(
            Long id,
            AssignmentRequest request) {

        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Assignment not found"));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() ->
                        new RuntimeException("Subject not found"));

        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() ->
                        new RuntimeException("Teacher not found"));

        assignment.setSubject(subject);
        assignment.setTeacher(teacher);
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setDueDate(request.getDueDate());
        assignment.setMaxMarks(request.getMaxMarks());

        assignment = assignmentRepository.save(assignment);

        return map(assignment);
    }

    public void deleteAssignment(Long id) {

        if (!assignmentRepository.existsById(id)) {
            throw new RuntimeException("Assignment not found");
        }

        assignmentRepository.deleteById(id);
    }

    private AssignmentResponse map(Assignment a) {

        return AssignmentResponse.builder()
                .id(a.getId())
                .subjectId(a.getSubject().getId())
                .subjectName(a.getSubject().getSubjectName())
                .teacherId(a.getTeacher().getId())
                .teacherName(
                        a.getTeacher().getFirstName()
                                + " "
                                + a.getTeacher().getLastName())
                .title(a.getTitle())
                .description(a.getDescription())
                .dueDate(a.getDueDate())
                .maxMarks(a.getMaxMarks())
                .build();
    }
}