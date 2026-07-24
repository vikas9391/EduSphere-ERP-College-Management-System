package com.collegeerp.Backend.student.entity;

import com.collegeerp.Backend.course.entity.Course;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_no", unique = true, nullable = false)
    private String admissionNo;

    @Column(name = "roll_number", unique = true)
    private String rollNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private String phone;

    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "admission_date")
    private LocalDate admissionDate;

    /**
     * The course/programme this student is admitted into. Nullable: existing students
     * predating this field have no value until an admin assigns one. This is separate
     * from - and does not replace - the per-subject relationship reachable via
     * Enrollment; that still governs which specific subjects (and therefore which
     * teachers) a student is connected to for a given semester. This field answers the
     * coarser "what course/batch is this student in" question directly, without having
     * to derive it from enrollment rows.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String city;

    private String state;

    private String country;

    private String pincode;

    @Column(name = "father_name")
    private String fatherName;

    @Column(name = "mother_name")
    private String motherName;

    @Column(name = "parent_phone")
    private String parentPhone;

    @Column(name = "parent_email")
    private String parentEmail;

    @Column(name = "blood_group")
    private String bloodGroup;

    private String category;

    private String nationality;

    @Column(name = "aadhaar_number")
    private String aadhaarNumber;

    @Column(name = "photo_url")
    private String photoUrl;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}