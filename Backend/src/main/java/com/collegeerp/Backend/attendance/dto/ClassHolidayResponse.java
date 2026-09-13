package com.collegeerp.Backend.attendance.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class ClassHolidayResponse {
    Long id;
    Long classId;
    String className;
    LocalDate holidayDate;
    String reason;
}
