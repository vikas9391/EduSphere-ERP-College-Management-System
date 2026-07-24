package com.collegeerp.Backend.schoolclass.entity;

import com.collegeerp.Backend.teacher.entity.Teacher;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A subject scoped to one {@link SchoolClass}. {@code enrollmentMode} controls whether
 * students are auto-enrolled ({@link EnrollmentMode#MANDATORY}) or self-enroll
 * ({@link EnrollmentMode#ELECTIVE}) - see {@code ClassSubjectService}.
 */
@Entity
@Table(name = "class_subjects",
       uniqueConstraints = @UniqueConstraint(columnNames = {"school_class_id", "subject_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id", nullable = false)
    private SchoolClass schoolClass;

    @Column(name = "subject_code", nullable = false)
    private String subjectCode;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @Column(nullable = false)
    private Integer credits;

    /** The subject-specific teacher - not necessarily the class owner. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(name = "enrollment_mode", nullable = false)
    @Enumerated(EnumType.STRING)
    private EnrollmentMode enrollmentMode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum EnrollmentMode {
        MANDATORY,
        ELECTIVE
    }
}
