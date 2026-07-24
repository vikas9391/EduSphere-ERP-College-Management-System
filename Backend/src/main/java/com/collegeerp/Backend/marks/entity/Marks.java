package com.collegeerp.Backend.marks.entity;

import com.collegeerp.Backend.examination.entity.ExamSchedule;
import com.collegeerp.Backend.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "marks",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"exam_schedule_id", "student_id"})
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Marks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_schedule_id", nullable = false)
    private ExamSchedule examSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private Integer internalMarks;

    @Column(nullable = false)
    private Integer externalMarks;

    @Column(nullable = false)
    private Integer totalMarks;

    @Column(nullable = false)
    private String grade;

    @Column(nullable = false)
    private Double gradePoint;

    @Column(nullable = false)
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
