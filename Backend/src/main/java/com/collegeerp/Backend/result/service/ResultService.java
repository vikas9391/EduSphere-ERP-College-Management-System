package com.collegeerp.Backend.result.service;

import com.collegeerp.Backend.marks.entity.Marks;
import com.collegeerp.Backend.marks.repository.MarksRepository;
import com.collegeerp.Backend.result.dto.OverallResultResponse;
import com.collegeerp.Backend.result.dto.SemesterResultResponse;
import com.collegeerp.Backend.result.dto.SubjectResultResponse;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ResultService {

    private final MarksRepository marksRepository;
    private final StudentRepository studentRepository;

    public ResultService(MarksRepository marksRepository, StudentRepository studentRepository) {
        this.marksRepository = marksRepository;
        this.studentRepository = studentRepository;
    }

    public SemesterResultResponse getSemesterResult(Long studentId, Integer semester, String academicYear) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<Marks> marksList = marksRepository.findPublishedByStudentAndSemester(studentId, semester, academicYear);

        if (marksList.isEmpty()) {
            throw new RuntimeException("No published results found for this student in the given semester");
        }

        return buildSemesterResult(student, semester, academicYear, marksList);
    }

    public OverallResultResponse getOverallResult(Long studentId) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<Marks> allMarks = marksRepository.findAllPublishedByStudent(studentId);

        if (allMarks.isEmpty()) {
            throw new RuntimeException("No published results found for this student");
        }

        Map<String, List<Marks>> bySemester = allMarks.stream()
                .collect(Collectors.groupingBy(m ->
                        m.getExamSchedule().getExam().getSemester() + "|" + m.getExamSchedule().getExam().getAcademicYear()));

        List<SemesterResultResponse> semesterResults = new ArrayList<>();

        for (Map.Entry<String, List<Marks>> entry : bySemester.entrySet()) {

            String[] parts = entry.getKey().split("\\|");
            Integer semester = Integer.valueOf(parts[0]);
            String academicYear = parts[1];

            semesterResults.add(buildSemesterResult(student, semester, academicYear, entry.getValue()));
        }

        semesterResults.sort(Comparator.comparing(SemesterResultResponse::getAcademicYear)
                .thenComparing(SemesterResultResponse::getSemester));

        int totalCredits = semesterResults.stream().mapToInt(SemesterResultResponse::getTotalCredits).sum();

        double weightedPoints = semesterResults.stream()
                .mapToDouble(s -> s.getSgpa() * s.getTotalCredits())
                .sum();

        double cgpa = totalCredits == 0 ? 0.0 : round(weightedPoints / totalCredits);

        boolean anyFail = semesterResults.stream().anyMatch(s -> "FAIL".equals(s.getResult()));

        return OverallResultResponse.builder()
                .studentId(student.getId())
                .studentName(fullName(student))
                .semesterResults(semesterResults)
                .totalCredits(totalCredits)
                .cgpa(cgpa)
                .overallResult(anyFail ? "FAIL" : "PASS")
                .build();
    }

    private SemesterResultResponse buildSemesterResult(Student student, Integer semester, String academicYear, List<Marks> marksList) {

        List<SubjectResultResponse> subjectResults = marksList.stream()
                .map(m -> SubjectResultResponse.builder()
                        .subjectId(m.getExamSchedule().getSubject().getId())
                        .subjectCode(m.getExamSchedule().getSubject().getSubjectCode())
                        .subjectName(m.getExamSchedule().getSubject().getSubjectName())
                        .credits(m.getExamSchedule().getSubject().getCredits())
                        .internalMarks(m.getInternalMarks())
                        .externalMarks(m.getExternalMarks())
                        .totalMarks(m.getTotalMarks())
                        .maxMarks(m.getExamSchedule().getMaxMarks())
                        .grade(m.getGrade())
                        .gradePoint(m.getGradePoint())
                        .build())
                .toList();

        int totalCredits = subjectResults.stream().mapToInt(SubjectResultResponse::getCredits).sum();

        double weightedPoints = subjectResults.stream()
                .mapToDouble(s -> s.getCredits() * s.getGradePoint())
                .sum();

        double sgpa = totalCredits == 0 ? 0.0 : round(weightedPoints / totalCredits);

        boolean anyFail = subjectResults.stream().anyMatch(s -> "F".equals(s.getGrade()));

        return SemesterResultResponse.builder()
                .studentId(student.getId())
                .studentName(fullName(student))
                .semester(semester)
                .academicYear(academicYear)
                .subjects(subjectResults)
                .totalCredits(totalCredits)
                .sgpa(sgpa)
                .result(anyFail ? "FAIL" : "PASS")
                .build();
    }

    private String fullName(Student student) {
        return student.getFirstName() + " " + (student.getLastName() != null ? student.getLastName() : "");
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
