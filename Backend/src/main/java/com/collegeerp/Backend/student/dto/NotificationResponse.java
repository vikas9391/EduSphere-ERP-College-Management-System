package com.collegeerp.Backend.student.dto;

import lombok.*;

import java.time.LocalDateTime;

/** A persisted notification item currently backed by received announcements. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private String title;
    private String message;
    private String type;
    private boolean read;
    private LocalDateTime createdAt;
}
