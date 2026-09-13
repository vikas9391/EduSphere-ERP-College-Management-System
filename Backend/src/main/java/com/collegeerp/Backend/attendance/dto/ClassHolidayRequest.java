package com.collegeerp.Backend.attendance.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ClassHolidayRequest {
    private Long classId;
    private LocalDate holidayDate;
    private String reason;
}
