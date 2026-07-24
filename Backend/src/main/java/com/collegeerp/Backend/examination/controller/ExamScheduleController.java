package com.collegeerp.Backend.examination.controller;

import com.collegeerp.Backend.examination.dto.ExamScheduleRequest;
import com.collegeerp.Backend.examination.dto.ExamScheduleResponse;
import com.collegeerp.Backend.examination.service.ExamScheduleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/exam-schedules")
public class ExamScheduleController {

    private final ExamScheduleService examScheduleService;

    public ExamScheduleController(ExamScheduleService examScheduleService) {
        this.examScheduleService = examScheduleService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ExamScheduleResponse createSchedule(@RequestBody ExamScheduleRequest request) {
        return examScheduleService.createSchedule(request);
    }

    @GetMapping("/exam/{examId}")
    public List<ExamScheduleResponse> getScheduleByExam(@PathVariable Long examId) {
        return examScheduleService.getScheduleByExam(examId);
    }

    @GetMapping("/{id}")
    public ExamScheduleResponse getSchedule(@PathVariable Long id) {
        return examScheduleService.getSchedule(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ExamScheduleResponse updateSchedule(@PathVariable Long id,
                                                @RequestBody ExamScheduleRequest request) {
        return examScheduleService.updateSchedule(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public String deleteSchedule(@PathVariable Long id) {

        examScheduleService.deleteSchedule(id);

        return "Exam schedule deleted successfully.";
    }
}
