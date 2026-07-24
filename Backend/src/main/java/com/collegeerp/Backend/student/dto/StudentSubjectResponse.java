package com.collegeerp.Backend.student.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSubjectResponse {

    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer credits;
    private Integer semester;

    private Long courseId;
    private String courseName;

    private Long teacherId;
    private String teacherName;
}
