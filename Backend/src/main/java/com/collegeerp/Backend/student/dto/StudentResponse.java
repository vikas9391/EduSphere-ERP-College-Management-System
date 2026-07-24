package com.collegeerp.Backend.student.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResponse {

    private Long id;

    private String admissionNo;

    private String rollNumber;

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private String gender;

    private LocalDate dateOfBirth;

    private LocalDate admissionDate;

    /**
     * Flat courseId/courseName pair rather than a nested Course object - mirrors how
     * CourseResponse itself exposes departmentId/departmentName instead of a nested
     * Department. Both are null when the student has no course assigned.
     */
    private Long courseId;

    private String courseName;

    private String address;

    private String city;

    private String state;

    private String country;

    private String pincode;

    private String fatherName;

    private String motherName;

    private String parentPhone;

    private String parentEmail;

    private String bloodGroup;

    private String category;

    private String nationality;

    private String photoUrl;

    private String status;
}