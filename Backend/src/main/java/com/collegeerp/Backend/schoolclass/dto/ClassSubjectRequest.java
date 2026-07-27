package com.collegeerp.Backend.schoolclass.dto;

import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSubjectRequest {

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

    @NotNull(message = "Teacher is required")
    private Long teacherId;

    @NotNull(message = "Enrollment mode is required")
    private ClassSubject.EnrollmentMode enrollmentMode;

    /**
     * Optional link to a formal curriculum Subject. Leave null for an informal
     * class-subject with no official backing. When provided, this class's roster
     * becomes an additional eligibility source for marks entry against that Subject.
     */
    private Long subjectId;
}
