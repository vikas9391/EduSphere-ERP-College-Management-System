package com.collegeerp.Backend.student.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableEntryResponse {

    private String startTime;
    private String endTime;
    private Long subjectId;
    private String subjectName;
    private String teacherName;
    private String room;
}
