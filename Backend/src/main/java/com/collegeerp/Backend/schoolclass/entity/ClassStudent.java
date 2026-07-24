package com.collegeerp.Backend.schoolclass.entity;

import com.collegeerp.Backend.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** One row per student on a {@link SchoolClass}'s roster. */
@Entity
@Table(name = "class_students",
       uniqueConstraints = @UniqueConstraint(columnNames = {"school_class_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id", nullable = false)
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "added_at")
    private LocalDateTime addedAt;
}
