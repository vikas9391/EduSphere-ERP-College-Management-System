package com.collegeerp.Backend.student.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;

/**
 * Payload for a student updating their own profile via {@code PUT /api/student/profile}.
 * <p>
 * Deliberately does NOT include {@code admissionNo}, {@code rollNumber}, {@code firstName},
 * {@code lastName}, {@code email}, or {@code password} - those are administrative/identity
 * fields a student cannot self-service change. Previously this endpoint reused the admin
 * {@code StudentRequest} DTO, which accepted (and silently ignored) those fields; a client
 * could reasonably assume submitting them would work. This dedicated DTO makes the actual
 * contract explicit.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfileUpdateRequest {

    @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Phone must be a valid phone number")
    private String phone;

    private String gender;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

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
