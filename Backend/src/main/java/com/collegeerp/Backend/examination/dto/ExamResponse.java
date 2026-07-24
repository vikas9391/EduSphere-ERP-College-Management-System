package com.collegeerp.Backend.examination.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamResponse {

    private Long id;

    private String examName;

    private String examType;

    private String academicYear;

    private Integer semester;

    private Long courseId;

    private String courseName;

    private LocalDate startDate;

    private LocalDate endDate;

}
