package com.collegeerp.Backend.assignment.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentRequest {

    private Long subjectId;

    private Long teacherId;

    private String title;

    private String description;

    private LocalDate dueDate;

    private Integer maxMarks;
}