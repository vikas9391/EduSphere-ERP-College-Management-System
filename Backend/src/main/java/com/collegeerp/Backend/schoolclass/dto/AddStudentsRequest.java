package com.collegeerp.Backend.schoolclass.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

/** Bulk-add students to a class's roster in one call. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddStudentsRequest {

    @NotEmpty(message = "At least one student id is required")
    private List<Long> studentIds;
}
