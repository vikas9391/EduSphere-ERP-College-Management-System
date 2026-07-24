package com.collegeerp.Backend.teacher.dto;

import lombok.*;

/**
 * A single entry in the teacher's "today's schedule" panel.
 * <b>See {@link com.collegeerp.Backend.teacher.service.TeacherScheduleService} - the
 * time slot and room are placeholder data, not read from any real timetable.</b>
 */
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
