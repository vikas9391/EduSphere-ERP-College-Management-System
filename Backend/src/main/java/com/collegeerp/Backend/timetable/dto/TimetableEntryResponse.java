package com.collegeerp.Backend.timetable.dto;

import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableEntryResponse {
    private Long id;
    private Long classSubjectId;
    private Long schoolClassId;
    private String schoolClassName;
    private String academicYear;
    private Integer semester;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Long teacherId;
    private String teacherName;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
}
