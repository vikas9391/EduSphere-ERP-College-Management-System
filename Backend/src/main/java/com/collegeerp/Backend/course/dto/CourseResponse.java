package com.collegeerp.Backend.course.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResponse {

    private Long id;

    private String courseCode;

    private String courseName;

    private Integer duration;

    private String description;

    private Long departmentId;

    private String departmentName;

}