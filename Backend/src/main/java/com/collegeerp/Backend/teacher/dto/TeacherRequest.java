package com.collegeerp.Backend.teacher.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * Create/update payload for a teacher. Validation mirrors the NOT NULL / UNIQUE
 * constraints on the {@code Teacher} entity so bad input is rejected with a 400
 * before it ever reaches the database.
 */
@Data
public class TeacherRequest {

    @NotBlank(message = "Employee ID is required")
    @Size(max = 50, message = "Employee ID must be at most 50 characters")
    private String employeeId;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be at most 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must be at most 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    /**
     * Required when creating a teacher (enforced in TeacherService, not here via @NotBlank,
     * since the same request type is reused for updates where a blank password means
     * "leave the existing password unchanged" - identical convention to StudentRequest).
     */
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Phone must be a valid phone number")
    private String phone;

    private String gender;

    private String qualification;

    private String specialization;

    @Min(value = 0, message = "Experience cannot be negative")
    @Max(value = 60, message = "Experience must be realistic")
    private Integer experience;

    @NotNull(message = "Joining date is required")
    @PastOrPresent(message = "Joining date cannot be in the future")
    private LocalDate joiningDate;
}
