package com.collegeerp.Backend.schoolclass.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolClassResponse {

    private Long id;
    private String name;
    private String academicYear;
    private Integer semester;
    private Integer maxSubjects;
    private Long teacherId;
    private String teacherName;
    private int studentCount;
    private int subjectCount;
}
