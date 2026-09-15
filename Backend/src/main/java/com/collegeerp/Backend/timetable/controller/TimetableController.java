package com.collegeerp.Backend.timetable.controller;

import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.timetable.dto.TimetableEntryRequest;
import com.collegeerp.Backend.timetable.dto.TimetableEntryResponse;
import com.collegeerp.Backend.timetable.dto.TimetableImportResponse;
import com.collegeerp.Backend.timetable.service.TimetableImportService;
import com.collegeerp.Backend.timetable.service.TimetableService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/timetable")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
public class TimetableController {

    private final TimetableService timetableService;
    private final TimetableImportService timetableImportService;

    public TimetableController(TimetableService timetableService, TimetableImportService timetableImportService) {
        this.timetableService = timetableService;
        this.timetableImportService = timetableImportService;
    }

    @PostMapping
    public TimetableEntryResponse create(Authentication authentication, @RequestBody TimetableEntryRequest request) {
        return timetableService.create(request, principal(authentication));
    }

    @PutMapping("/{id}")
    public TimetableEntryResponse update(Authentication authentication, @PathVariable Long id, @RequestBody TimetableEntryRequest request) {
        return timetableService.update(id, request, principal(authentication));
    }

    @DeleteMapping("/{id}")
    public void delete(Authentication authentication, @PathVariable Long id) {
        timetableService.delete(id, principal(authentication));
    }

    @GetMapping("/class-subject/{classSubjectId}")
    public List<TimetableEntryResponse> getForClassSubject(Authentication authentication, @PathVariable Long classSubjectId) {
        return timetableService.getForClassSubject(classSubjectId, principal(authentication));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('TEACHER')")
    public List<TimetableEntryResponse> mine(Authentication authentication) {
        return timetableService.getMine(principal(authentication));
    }

    @PostMapping(value = "/import/inspect", consumes = "multipart/form-data")
    public TimetableImportResponse inspectImport(Authentication authentication, @RequestPart("file") MultipartFile file) {
        return timetableImportService.inspect(file, principal(authentication));
    }

    private UserPrincipal principal(Authentication authentication) {
        return (UserPrincipal) authentication.getPrincipal();
    }
}
