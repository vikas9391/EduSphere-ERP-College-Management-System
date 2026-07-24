package com.collegeerp.Backend.subject.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectRequest {

    @NotBlank(message = "Subject code is required")
    @Size(max = 30, message = "Subject code must be at most 30 characters")
    private String subjectCode;

    @NotBlank(message = "Subject name is required")
    @Size(max = 150, message = "Subject name must be at most 150 characters")
    private String subjectName;

    @NotNull(message = "Credits is required")
    @Min(value = 1, message = "Credits must be at least 1")
    @Max(value = 20, message = "Credits must be realistic")
    private Integer credits;

    @NotNull(message = "Semester is required")
    @Min(value = 1, message = "Semester must be at least 1")
    @Max(value = 12, message = "Semester must be realistic")
    private Integer semester;

    @NotNull(message = "Course is required")
    private Long courseId;

    @NotNull(message = "Teacher is required")
    private Long teacherId;
}
