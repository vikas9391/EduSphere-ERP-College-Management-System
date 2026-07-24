package com.collegeerp.Backend.examination.controller;

import com.collegeerp.Backend.examination.dto.ExamRequest;
import com.collegeerp.Backend.examination.dto.ExamResponse;
import com.collegeerp.Backend.examination.service.ExamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ExamResponse createExam(@RequestBody ExamRequest request) {
        return examService.createExam(request);
    }

    @GetMapping
    public List<ExamResponse> getAllExams() {
        return examService.getAllExams();
    }

    @GetMapping("/{id}")
    public ExamResponse getExam(@PathVariable Long id) {
        return examService.getExam(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ExamResponse updateExam(@PathVariable Long id,
                                    @RequestBody ExamRequest request) {
        return examService.updateExam(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public String deleteExam(@PathVariable Long id) {

        examService.deleteExam(id);

        return "Exam deleted successfully.";
    }
}
