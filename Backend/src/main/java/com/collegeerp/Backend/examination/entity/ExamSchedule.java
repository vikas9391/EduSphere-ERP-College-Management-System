package com.collegeerp.Backend.examination.entity;

import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.subject.entity.Subject;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "exam_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    /**
     * Exact taught subject instance for new class-scoped exam schedules. Kept nullable while
     * legacy Subject-only schedules are reconciled.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_subject_id")
    private ClassSubject classSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invigilator_id")
    private User invigilator;

    @Column(nullable = false)
    private LocalDate examDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    private String room;

    @Column(nullable = false)
    private Integer maxMarks;

    private LocalDateTime createdAt;
}
