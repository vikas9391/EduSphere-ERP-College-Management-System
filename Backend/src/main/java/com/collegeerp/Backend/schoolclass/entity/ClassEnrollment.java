package com.collegeerp.Backend.schoolclass.entity;

import com.collegeerp.Backend.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * The actual student <-> {@link ClassSubject} connection. Created automatically for
 * MANDATORY subjects (when the subject is created, and when a new student joins a class
 * that already has MANDATORY subjects), or by the student themself for ELECTIVE ones -
 * {@code source} records which.
 */
@Entity
@Table(name = "class_enrollments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"class_subject_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_subject_id", nullable = false)
    private ClassSubject classSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Source source;

    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;

    public enum Source {
        AUTO,
        SELF
    }
}
