package com.collegeerp.Backend.result.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverallResultResponse {

    private Long studentId;

    private String studentName;

    private List<SemesterResultResponse> semesterResults;

    private Integer totalCredits;

    private Double cgpa;

    private String overallResult;

}
