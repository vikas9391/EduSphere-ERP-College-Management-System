package com.collegeerp.Backend.result.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectResultResponse {

    private Long subjectId;

    private String subjectCode;

    private String subjectName;

    private Integer credits;

    private Integer internalMarks;

    private Integer externalMarks;

    private Integer totalMarks;

    private Integer maxMarks;

    private String grade;

    private Double gradePoint;

}
