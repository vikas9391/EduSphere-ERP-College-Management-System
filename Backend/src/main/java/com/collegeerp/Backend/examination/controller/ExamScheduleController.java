package com.collegeerp.Backend.examination.controller;

import com.collegeerp.Backend.examination.dto.ExamScheduleRequest;
import com.collegeerp.Backend.examination.dto.ExamScheduleResponse;
import com.collegeerp.Backend.examination.service.ExamScheduleService;
import com.collegeerp.Backend.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam-schedules")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
public class ExamScheduleController {

    private final ExamScheduleService examScheduleService;

    public ExamScheduleController(ExamScheduleService examScheduleService) {
        this.examScheduleService = examScheduleService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping
    public ExamScheduleResponse createSchedule(@RequestBody ExamScheduleRequest request) {
        return examScheduleService.createSchedule(request);
    }

    @GetMapping("/exam/{examId}")
    public List<ExamScheduleResponse> getScheduleByExam(
            Authentication authentication,
            @PathVariable Long examId) {
        return examScheduleService.getScheduleByExam(examId, principal(authentication));
    }

    @GetMapping("/{id}")
    public ExamScheduleResponse getSchedule(
            Authentication authentication,
            @PathVariable Long id) {
        return examScheduleService.getSchedule(id, principal(authentication));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PutMapping("/{id}")
    public ExamScheduleResponse updateSchedule(@PathVariable Long id,
                                                @RequestBody ExamScheduleRequest request) {
        return examScheduleService.updateSchedule(id, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public String deleteSchedule(@PathVariable Long id) {
        examScheduleService.deleteSchedule(id);
        return "Exam schedule deleted successfully.";
    }

    private UserPrincipal principal(Authentication authentication) {
        return (UserPrincipal) authentication.getPrincipal();
    }
}
