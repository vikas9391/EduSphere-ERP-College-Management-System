package com.collegeerp.Backend.marks.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarksResponse {

    private Long id;

    private Long examScheduleId;

    private Long examId;

    private String examName;

    private Long subjectId;

    private String subjectName;

    private Long studentId;

    private String studentName;

    private Integer internalMarks;

    private Integer externalMarks;

    private Integer totalMarks;

    private Integer maxMarks;

    private String grade;

    private Double gradePoint;

    private String status;

}
