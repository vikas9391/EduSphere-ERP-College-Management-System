package com.collegeerp.Backend.examination.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamScheduleRequest {

    @NotNull(message = "Exam is required")
    private Long examId;

    @NotNull(message = "Class subject is required")
    private Long classSubjectId;

    private Long invigilatorId;
    private LocalDate examDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private Integer maxMarks;
}
