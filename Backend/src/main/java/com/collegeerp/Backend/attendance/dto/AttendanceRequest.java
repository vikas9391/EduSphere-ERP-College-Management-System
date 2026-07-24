package com.collegeerp.Backend.attendance.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRequest {

    private Long enrollmentId;

    private LocalDate attendanceDate;

    private String status;

    private String remarks;

}