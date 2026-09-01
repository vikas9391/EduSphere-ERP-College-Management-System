package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.assignment.entity.Assignment;
import com.collegeerp.Backend.assignment.entity.AssignmentSubmission;
import com.collegeerp.Backend.assignment.repository.AssignmentRepository;
import com.collegeerp.Backend.assignment.repository.AssignmentSubmissionRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassEnrollment;
import com.collegeerp.Backend.schoolclass.repository.ClassEnrollmentRepository;
import com.collegeerp.Backend.student.dto.StudentAssignmentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StudentAssignmentService {

    private static final String NOT_SUBMITTED = "NOT_SUBMITTED";

    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;

    public StudentAssignmentService(
            ClassEnrollmentRepository classEnrollmentRepository,
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository) {
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
    }

    public List<StudentAssignmentResponse> getAssignments(Long studentId) {
        List<ClassEnrollment> enrollments = classEnrollmentRepository.findAllByStudentId(studentId);

        List<Long> classSubjectIds = enrollments.stream()
                .map(ClassEnrollment::getClassSubject)
                .filter(java.util.Objects::nonNull)
                .map(cs -> cs.getId())
                .distinct()
                .toList();

        List<Long> subjectIds = enrollments.stream()
                .map(ClassEnrollment::getClassSubject)
                .filter(java.util.Objects::nonNull)
                .map(cs -> cs.getSubject())
                .filter(java.util.Objects::nonNull)
                .map(s -> s.getId())
                .distinct()
                .toList();

        if (classSubjectIds.isEmpty() && subjectIds.isEmpty()) {
            return List.of();
        }

        List<Assignment> assignments = assignmentRepository.findForStudentClassSubjects(
                classSubjectIds.isEmpty() ? List.of(-1L) : classSubjectIds,
                subjectIds.isEmpty() ? List.of(-1L) : subjectIds);

        Map<Long, AssignmentSubmission> submissionsByAssignmentId = submissionRepository.findByStudentId(studentId)
                .stream()
                .collect(Collectors.toMap(
                        s -> s.getAssignment().getId(),
                        Function.identity(),
                        (first, ignored) -> first
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
                .submissionUrl(submission.getSubmissionUrl())
                .marksObtained(submission.getMarks())
                .feedback(submission.getFeedback())
                .build();
    }
}
