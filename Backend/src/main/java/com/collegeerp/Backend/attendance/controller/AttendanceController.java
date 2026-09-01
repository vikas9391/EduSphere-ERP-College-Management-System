package com.collegeerp.Backend.attendance.controller;

import com.collegeerp.Backend.attendance.dto.AttendanceRequest;
import com.collegeerp.Backend.attendance.dto.AttendanceResponse;
import com.collegeerp.Backend.attendance.service.AttendanceService;
import com.collegeerp.Backend.attendance.service.TeacherAttendanceQueryService;
import com.collegeerp.Backend.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceService attendanceService;
    private final TeacherAttendanceQueryService teacherAttendanceQueryService;

    public AttendanceController(
            AttendanceService attendanceService,
            TeacherAttendanceQueryService teacherAttendanceQueryService) {
        this.attendanceService = attendanceService;
        this.teacherAttendanceQueryService = teacherAttendanceQueryService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @PostMapping
    public AttendanceResponse createAttendance(@RequestBody AttendanceRequest request) {
        return attendanceService.createAttendance(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @GetMapping
    public List<AttendanceResponse> getAllAttendance(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        if ("TEACHER".equalsIgnoreCase(principal.getRole())) {
            return teacherAttendanceQueryService.getTeacherAttendance(principal.getId());
        }
        return attendanceService.getAllAttendance();
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me")
    public List<AttendanceResponse> getMyAttendance() {
        return attendanceService.getMyAttendance();
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me/summary")
    public com.collegeerp.Backend.attendance.dto.StudentAttendanceSummaryResponse getMyAttendanceSummary() {
        return attendanceService.getMyAttendanceSummary();
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping("/student/{studentId}")
    public List<AttendanceResponse> getStudentAttendance(@PathVariable Long studentId) {
        return attendanceService.getStudentAttendance(studentId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @GetMapping("/{id}")
    public AttendanceResponse getAttendance(@PathVariable Long id) {
        return attendanceService.getAttendance(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @PutMapping("/{id}")
    public AttendanceResponse updateAttendance(@PathVariable Long id, @RequestBody AttendanceRequest request) {
        return attendanceService.updateAttendance(id, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @DeleteMapping("/{id}")
    public void deleteAttendance(@PathVariable Long id) {
        attendanceService.deleteAttendance(id);
    }
}
