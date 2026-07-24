package com.collegeerp.Backend.teacher.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * A single announcement item returned by the placeholder announcements endpoint.
 * <b>See {@link com.collegeerp.Backend.teacher.service.TeacherAnnouncementService} for
 * why this is mock data.</b> Deliberately a teacher-local type rather than reusing
 * {@code student.dto.NotificationResponse}, even though the shape is identical, so the
 * teacher and student packages stay independent of one another.
 */
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
