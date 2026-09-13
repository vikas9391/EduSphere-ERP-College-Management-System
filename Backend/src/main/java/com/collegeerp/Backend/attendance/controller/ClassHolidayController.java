package com.collegeerp.Backend.attendance.controller;

import com.collegeerp.Backend.attendance.dto.ClassHolidayRequest;
import com.collegeerp.Backend.attendance.dto.ClassHolidayResponse;
import com.collegeerp.Backend.attendance.service.ClassHolidayService;
import com.collegeerp.Backend.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance/holidays")
public class ClassHolidayController {
    private final ClassHolidayService holidayService;

    public ClassHolidayController(ClassHolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER','STUDENT')")
    @GetMapping
    public List<ClassHolidayResponse> list(@RequestParam Long classId, Authentication authentication) {
        return holidayService.list(classId, (UserPrincipal) authentication.getPrincipal());
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @PostMapping
    public ClassHolidayResponse create(@RequestBody ClassHolidayRequest request, Authentication authentication) {
        return holidayService.create(request, (UserPrincipal) authentication.getPrincipal());
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication authentication) {
        holidayService.delete(id, (UserPrincipal) authentication.getPrincipal());
    }
}
