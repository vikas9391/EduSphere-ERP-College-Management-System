package com.collegeerp.Backend.student.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableEntryResponse {

    private Long classSubjectId;
    /** Nullable when the class subject is not linked to the formal curriculum Subject. */
    private Long subjectId;
    private String subjectName;
    private String teacherName;
    private String startTime;
    private String endTime;
    private String room;
}
