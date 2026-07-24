package com.collegeerp.Backend.marks.controller;

import com.collegeerp.Backend.marks.dto.MarksRequest;
import com.collegeerp.Backend.marks.dto.MarksResponse;
import com.collegeerp.Backend.marks.service.MarksService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/marks")
public class MarksController {

    private final MarksService marksService;

    public MarksController(MarksService marksService) {
        this.marksService = marksService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping
    public MarksResponse enterMarks(@RequestBody MarksRequest request) {
        return marksService.enterMarks(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PutMapping("/{id}")
    public MarksResponse updateMarks(@PathVariable Long id,
                                      @RequestBody MarksRequest request) {
        return marksService.updateMarks(id, request);
    }

    @GetMapping("/exam-schedule/{examScheduleId}")
    public List<MarksResponse> getMarksByExamSchedule(@PathVariable Long examScheduleId) {
        return marksService.getMarksByExamSchedule(examScheduleId);
    }

    @GetMapping("/{id}")
    public MarksResponse getMarks(@PathVariable Long id) {
        return marksService.getMarks(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PutMapping("/{id}/publish")
    public MarksResponse publishMarks(@PathVariable Long id) {
        return marksService.publishMarks(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PutMapping("/exam-schedule/{examScheduleId}/publish")
    public List<MarksResponse> publishMarksForExamSchedule(@PathVariable Long examScheduleId) {
        return marksService.publishMarksForExamSchedule(examScheduleId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping("/{id}")
    public String deleteMarks(@PathVariable Long id) {

        marksService.deleteMarks(id);

        return "Marks record deleted successfully.";
    }
}
