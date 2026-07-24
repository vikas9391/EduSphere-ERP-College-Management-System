package com.collegeerp.Backend.schoolclass.entity;

import com.collegeerp.Backend.teacher.entity.Teacher;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A teacher-owned batch/section: a name plus an academic year/semester, a roster of
 * students ({@link ClassStudent}), and a set of subjects ({@link ClassSubject}).
 * Deliberately not linked to {@code Course} - see V17 migration comment for why.
 */
@Entity
@Table(name = "school_classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @Column(nullable = false)
    private Integer semester;

    /** Fixed cap on subject count for this class. Null means uncapped. */
    @Column(name = "max_subjects")
    private Integer maxSubjects;

    /** The teacher who owns/manages this class. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
