package com.collegeerp.Backend.examination.service;

import com.collegeerp.Backend.course.entity.Course;
import com.collegeerp.Backend.course.repository.CourseRepository;
import com.collegeerp.Backend.examination.dto.ExamRequest;
import com.collegeerp.Backend.examination.dto.ExamResponse;
import com.collegeerp.Backend.examination.entity.Exam;
import com.collegeerp.Backend.examination.repository.ExamRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final CourseRepository courseRepository;

    public ExamService(ExamRepository examRepository, CourseRepository courseRepository) {
        this.examRepository = examRepository;
        this.courseRepository = courseRepository;
    }

    public ExamResponse createExam(ExamRequest request) {

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("Exam end date cannot be before start date");
        }

        Exam exam = Exam.builder()
                .examName(request.getExamName())
                .examType(request.getExamType())
                .academicYear(request.getAcademicYear())
                .semester(request.getSemester())
                .course(course)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdAt(LocalDateTime.now())
                .build();

        exam = examRepository.save(exam);

        return map(exam);
    }

    public List<ExamResponse> getAllExams() {

        return examRepository.findAllWithCourse()
                .stream()
                .map(this::map)
                .toList();
    }

    public ExamResponse getExam(Long id) {

        return map(examRepository.findByIdWithCourse(id)
                .orElseThrow(() -> new RuntimeException("Exam not found")));
    }

    public ExamResponse updateExam(Long id, ExamRequest request) {

        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("Exam end date cannot be before start date");
        }

        exam.setExamName(request.getExamName());
        exam.setExamType(request.getExamType());
        exam.setAcademicYear(request.getAcademicYear());
        exam.setSemester(request.getSemester());
        exam.setCourse(course);
        exam.setStartDate(request.getStartDate());
        exam.setEndDate(request.getEndDate());

        exam = examRepository.save(exam);

        return map(exam);
    }

    public void deleteExam(Long id) {

        if (!examRepository.existsById(id)) {
            throw new RuntimeException("Exam not found");
        }

        examRepository.deleteById(id);
    }

    private ExamResponse map(Exam e) {

        return ExamResponse.builder()
                .id(e.getId())
                .examName(e.getExamName())
                .examType(e.getExamType())
                .academicYear(e.getAcademicYear())
                .semester(e.getSemester())
                .courseId(e.getCourse().getId())
                .courseName(e.getCourse().getCourseName())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .build();
    }
}
