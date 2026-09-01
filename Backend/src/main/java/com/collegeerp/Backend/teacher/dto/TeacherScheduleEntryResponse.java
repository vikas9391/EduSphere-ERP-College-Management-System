package com.collegeerp.Backend.teacher.dto;

import lombok.*;

/** A single real timetable entry in the teacher's "today's schedule" panel. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherScheduleEntryResponse {

    private Long subjectId;
    private String subjectName;
    private String startTime;
    private String endTime;
    private String room;
}
