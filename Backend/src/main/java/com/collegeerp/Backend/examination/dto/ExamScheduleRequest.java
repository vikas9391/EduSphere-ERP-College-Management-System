package com.collegeerp.Backend.examination.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamScheduleRequest {

    private Long examId;

    private Long subjectId;

    private Long invigilatorId;

    private LocalDate examDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String room;

    private Integer maxMarks;

}
