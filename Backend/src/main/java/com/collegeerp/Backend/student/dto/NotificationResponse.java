package com.collegeerp.Backend.student.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * A single notification item returned by the placeholder notifications endpoint.
 * <b>See {@code StudentNotificationService} for why this is mock data.</b>
 */
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
