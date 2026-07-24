package com.collegeerp.Backend.result.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemesterResultResponse {

    private Long studentId;

    private String studentName;

    private Integer semester;

    private String academicYear;

    private List<SubjectResultResponse> subjects;

    private Integer totalCredits;

    private Double sgpa;

    private String result;

}
