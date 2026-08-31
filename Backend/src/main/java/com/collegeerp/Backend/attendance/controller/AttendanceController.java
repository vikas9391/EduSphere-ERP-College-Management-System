package com.collegeerp.Backend.attendance.controller;

import com.collegeerp.Backend.attendance.dto.AttendanceRequest;
import com.collegeerp.Backend.attendance.dto.AttendanceResponse;
import com.collegeerp.Backend.attendance.service.AttendanceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceService attendanceService;
    public AttendanceController(AttendanceService attendanceService) { this.attendanceService = attendanceService; }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @PostMapping
    public AttendanceResponse createAttendance(@RequestBody AttendanceRequest request) { return attendanceService.createAttendance(request); }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @GetMapping
    public List<AttendanceResponse> getAllAttendance() { return attendanceService.getAllAttendance(); }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER','STUDENT')")
    @GetMapping("/me")
    public List<AttendanceResponse> getMyAttendance() { return attendanceService.getMyAttendance(); }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STUDENT')")
    @GetMapping("/me/summary")
    public com.collegeerp.Backend.attendance.dto.StudentAttendanceSummaryResponse getMyAttendanceSummary() {
        return attendanceService.getMyAttendanceSummary();
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @GetMapping("/student/{studentId}")
    public List<AttendanceResponse> getStudentAttendance(@PathVariable Long studentId) { return attendanceService.getStudentAttendance(studentId); }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @GetMapping("/{id}")
    public AttendanceResponse getAttendance(@PathVariable Long id) { return attendanceService.getAttendance(id); }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','TEACHER')")
    @PutMapping("/{id}")
    public AttendanceResponse updateAttendance(@PathVariable Long id, @RequestBody AttendanceRequest request) { return attendanceService.updateAttendance(id, request); }

    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @DeleteMapping("/{id}")
    public void deleteAttendance(@PathVariable Long id) { attendanceService.deleteAttendance(id); }
}