package com.collegeerp.Backend.student.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Response for {@code GET /api/student/timetable}.
 * <p>
 * <b>This is placeholder/mock data.</b> There is no {@code Timetable}/{@code Period} entity or
 * table anywhere in the schema - the ERP currently has no concept of scheduled class periods,
 * only exam schedules ({@code ExamSchedule}). {@code StudentTimetableService} deterministically
 * distributes the student's actual enrolled subjects across a Mon-Fri grid with fixed time
 * slots so the endpoint returns something structurally correct and stable for a given student,
 * but the day/time/room assignments are NOT read from any real scheduling data and must not be
 * treated as authoritative. Replace this with a real query once a Timetable module exists.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentTimetableResponse {

    private boolean placeholder;
    private String note;

    /** Keyed by day name (MONDAY..FRIDAY), each value ordered by start time. */
    private Map<String, List<TimetableEntryResponse>> schedule;
}
