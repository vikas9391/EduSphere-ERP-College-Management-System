package com.collegeerp.Backend.student.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

/** Response for the authenticated student's real ClassSubject-backed timetable. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentTimetableResponse {

    /** Retained for API compatibility; false for repository-backed timetable data. */
    private boolean placeholder;
    private String note;

    /** Keyed by day name (MONDAY..SUNDAY), each value ordered by start time. */
    private Map<String, List<TimetableEntryResponse>> schedule;
}
