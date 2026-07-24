package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.result.dto.OverallResultResponse;
import com.collegeerp.Backend.result.service.ResultService;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Self-service results view for the logged-in student. Deliberately delegates to the existing
 * {@link ResultService} (untouched - the {@code result} package hasn't been through its own
 * refactor pass yet, and this session is scoped to the student module only) rather than
 * duplicating its SGPA/CGPA aggregation logic.
 * <p>
 * {@code ResultService.getOverallResult} throws a raw {@code RuntimeException} for two very
 * different situations: "Student not found" (a real error - should never happen here since
 * {@code studentId} always comes from the authenticated JWT principal, but flagged just in
 * case) and "No published results found for this student" (a completely normal state for a
 * new student, not an error). Since {@code result} hasn't been refactored to throw distinct
 * exception types yet, this wrapper distinguishes the two by message content rather than
 * silently treating every failure as "no results yet".
 */
@Service
@Transactional(readOnly = true)
public class StudentResultService {

    private static final Logger log = LoggerFactory.getLogger(StudentResultService.class);

    private final ResultService resultService;
    private final StudentRepository studentRepository;

    public StudentResultService(ResultService resultService, StudentRepository studentRepository) {
        this.resultService = resultService;
        this.studentRepository = studentRepository;
    }

    public OverallResultResponse getResults(Long studentId) {
        try {
            return resultService.getOverallResult(studentId);
        } catch (RuntimeException e) {
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.contains("No published results")) {
                log.debug("No published results yet for student id={}", studentId);
                return emptyResult(studentId);
            }
            // Anything else (e.g. "Student not found") is a genuine error - let it propagate
            // to GlobalExceptionHandler's generic-RuntimeException fallback rather than
            // masking it as an empty result.
            throw e;
        }
    }

    private OverallResultResponse emptyResult(Long studentId) {
        String name = studentRepository.findById(studentId)
                .map(Student::getFirstName)
                .orElse(null);

        return OverallResultResponse.builder()
                .studentId(studentId)
                .studentName(name)
                .semesterResults(List.of())
                .totalCredits(0)
                .cgpa(0.0)
                .overallResult("NO_RESULTS_YET")
                .build();
    }
}
