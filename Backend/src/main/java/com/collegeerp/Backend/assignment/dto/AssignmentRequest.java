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

    /** Optional for legacy clients; new assignments should provide the class subject. */
    private Long classSubjectId;

    private Long teacherId;

    private String title;

    private String description;

    private LocalDate dueDate;

    private Integer maxMarks;
}