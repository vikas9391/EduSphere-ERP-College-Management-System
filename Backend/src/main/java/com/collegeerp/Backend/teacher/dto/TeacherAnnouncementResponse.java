package com.collegeerp.Backend.teacher.dto;

import lombok.*;

import java.time.LocalDateTime;

/** A received persisted announcement shown on the teacher dashboard. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherAnnouncementResponse {

    private Long id;
    private String title;
    private String body;
    private LocalDateTime createdAt;
}
