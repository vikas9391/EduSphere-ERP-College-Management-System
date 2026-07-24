package com.collegeerp.Backend.schoolclass.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolClassRequest {

    @NotBlank(message = "Class name is required")
    @Size(max = 150, message = "Class name must be at most 150 characters")
    private String name;

    @NotBlank(message = "Academic year is required")
    @Size(max = 20, message = "Academic year must be at most 20 characters")
    private String academicYear;

    @NotNull(message = "Semester is required")
    @Min(value = 1, message = "Semester must be at least 1")
    @Max(value = 12, message = "Semester must be realistic")
    private Integer semester;

    /** Fixed cap on how many subjects this class may have. Optional - null means uncapped. */
    @Min(value = 1, message = "Max subjects must be at least 1 if set")
    private Integer maxSubjects;
}
