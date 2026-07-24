package com.collegeerp.Backend.subject.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectResponse {

    private Long id;

    private String subjectCode;

    private String subjectName;

    private Integer credits;

    private Integer semester;

    private Long courseId;

    private String courseName;

    private Long teacherId;

    private String teacherName;
}