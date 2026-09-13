package com.collegeerp.Backend.attendance.entity;

import com.collegeerp.Backend.schoolclass.entity.SchoolClass;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_holidays", uniqueConstraints = @UniqueConstraint(columnNames = {"school_class_id", "holiday_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassHoliday {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_class_id", nullable = false)
    private SchoolClass schoolClass;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(length = 255)
    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
