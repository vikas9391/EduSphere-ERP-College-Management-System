package com.collegeerp.Backend.announcement.entity;

import com.collegeerp.Backend.common.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 40)
    private AudienceType audienceType;

    @Column(name = "audience_id")
    private Long audienceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum AudienceType {
        ALL_STUDENTS,
        ALL_TEACHERS,
        CLASS_STUDENTS,
        CLASS_TEACHERS,
        DEPARTMENT_STUDENTS,
        DEPARTMENT_TEACHERS
    }
}
