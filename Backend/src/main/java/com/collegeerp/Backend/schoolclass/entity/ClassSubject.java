package com.collegeerp.Backend.schoolclass.entity;

import com.collegeerp.Backend.subject.entity.Subject;
import com.collegeerp.Backend.common.User;
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
    private User teacher;

    @Column(name = "enrollment_mode", nullable = false)
    @Enumerated(EnumType.STRING)
    private EnrollmentMode enrollmentMode;

    /**
     * Optional link to the formal curriculum {@link Subject}. Null for purely informal
     * class-subjects (e.g. an ELECTIVE study group with no official backing). When set,
     * this class's roster ({@code class_enrollments}) becomes an additional source of
     * truth for marks-entry eligibility against that Subject - see
     * {@code MarksService#getEligibleStudents}. Deliberately optional rather than
     * mandatory: see the V17 migration comment for why Classes stay decoupled from the
     * formal Course/Subject structure by default.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum EnrollmentMode {
        MANDATORY,
        ELECTIVE
    }
}
