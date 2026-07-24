package com.collegeerp.Backend.examination.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamRequest {

    private String examName;

    private String examType;

    private String academicYear;

    private Integer semester;

    private Long courseId;

    private LocalDate startDate;

    private LocalDate endDate;

}
