package com.collegeerp.Backend.assignment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentRequest {

    @NotNull(message = "Class subject is required")
    private Long classSubjectId;

    private String title;
    private String description;
    private LocalDate dueDate;
    private Integer maxMarks;
}
