package com.collegeerp.Backend.assignment.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentResponse {

    private Long id;

    private Long subjectId;

    private String subjectName;

    private Long teacherId;

    private String teacherName;

    private String title;

    private String description;

    private LocalDate dueDate;

    private Integer maxMarks;
}