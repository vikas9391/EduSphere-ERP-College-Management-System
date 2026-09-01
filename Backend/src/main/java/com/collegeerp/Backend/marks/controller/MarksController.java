package com.collegeerp.Backend.marks.controller;

import com.collegeerp.Backend.marks.dto.EligibleStudentResponse;
import com.collegeerp.Backend.marks.dto.MarksRequest;
import com.collegeerp.Backend.marks.dto.MarksResponse;
import com.collegeerp.Backend.marks.service.MarksService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marks")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
public class MarksController {

    private final MarksService marksService;

    public MarksController(MarksService marksService) {
        this.marksService = marksService;
    }

    @PostMapping
    public MarksResponse enterMarks(@RequestBody MarksRequest request) {
        return marksService.enterMarks(request);
    }

    @PutMapping("/{id}")
    public MarksResponse updateMarks(@PathVariable Long id,
                                      @RequestBody MarksRequest request) {
        return marksService.updateMarks(id, request);
    }

    @GetMapping("/exam-schedule/{examScheduleId}")
    public List<MarksResponse> getMarksByExamSchedule(@PathVariable Long examScheduleId) {
        return marksService.getMarksByExamSchedule(examScheduleId);
    }

    @GetMapping("/exam-schedule/{examScheduleId}/eligible-students")
    public List<EligibleStudentResponse> getEligibleStudents(@PathVariable Long examScheduleId) {
        return marksService.getEligibleStudents(examScheduleId);
    }

    @GetMapping("/{id}")
    public MarksResponse getMarks(@PathVariable Long id) {
        return marksService.getMarks(id);
    }

    @PutMapping("/{id}/publish")
    public MarksResponse publishMarks(@PathVariable Long id) {
        return marksService.publishMarks(id);
    }

    @PutMapping("/exam-schedule/{examScheduleId}/publish")
    public List<MarksResponse> publishMarksForExamSchedule(@PathVariable Long examScheduleId) {
        return marksService.publishMarksForExamSchedule(examScheduleId);
    }

    @DeleteMapping("/{id}")
    public String deleteMarks(@PathVariable Long id) {
        marksService.deleteMarks(id);
        return "Marks record deleted successfully.";
    }
}
