package com.collegeerp.Backend.student.auth;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfileResponse {

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