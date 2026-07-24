package com.collegeerp.Backend.student.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Admin create/update payload for a student. {@code password} is required when creating
 * a student but optional when updating one (an update with a blank password leaves the
 * existing password unchanged) - {@code @Size} only validates non-null values, so it's
 * safe to apply here even though the field is conditionally required; {@code StudentService}
 * enforces the "required on create" rule explicitly.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRequest {

    @NotBlank(message = "Admission number is required")
    private String admissionNo;

    private String rollNumber;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Phone must be a valid phone number")
    private String phone;

    private String gender;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @PastOrPresent(message = "Admission date cannot be in the future")
    private LocalDate admissionDate;

    /**
     * The course/programme this student belongs to. Optional (not @NotNull): existing
     * students created before this field existed have none, and an admin may legitimately
     * admit a student before finalizing their course placement. When provided,
     * {@code StudentService} validates it references a real course. Sending null on an
     * update clears the course, matching how every other optional field here behaves
     * (unlike password/aadhaarNumber, which have "blank means unchanged" semantics).
     */
    private Long courseId;

    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;

    private String fatherName;
    private String motherName;

    @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Parent phone must be a valid phone number")
    private String parentPhone;

    @Email(message = "Parent email must be a valid email address")
    private String parentEmail;

    private String bloodGroup;
    private String category;
    private String nationality;

    private String aadhaarNumber;

    private String photoUrl;
}
