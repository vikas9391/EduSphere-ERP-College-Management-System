package com.collegeerp.Backend.examination.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamScheduleResponse {

    private Long id;

    private Long examId;

    private String examName;

    private Long subjectId;

    private String subjectName;

    private Long invigilatorId;

    private String invigilatorName;

    private LocalDate examDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String room;

    private Integer maxMarks;

}
