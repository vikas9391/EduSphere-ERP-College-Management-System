package com.collegeerp.Backend.teacher.service;

import com.collegeerp.Backend.teacher.dto.TeacherAnnouncementResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <b>PLACEHOLDER / MOCK DATA.</b>
 * <p>
 * There is no Announcement/Notification entity, table, or module anywhere in this codebase
 * (see {@code StudentNotificationService} for the same gap on the student side). Returns a
 * small, fixed, in-memory list rather than reading from any database - nothing here is
 * persisted or teacher-specific; every teacher currently sees the same mock announcements.
 * This was previously hard-coded directly in the frontend's TeacherDashboard component (see
 * README_PROGRESS.md); moving it server-side keeps the placeholder in one place. Replace
 * this class's body with a real repository-backed query once an Announcement module exists.
 */
@Service
public class TeacherAnnouncementService {

    public List<TeacherAnnouncementResponse> getAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                TeacherAnnouncementResponse.builder()
                        .id(1L)
                        .title("Mid-semester grades due Friday")
                        .body("Please submit all pending evaluations before the Friday deadline.")
                        .createdAt(now.minusHours(2))
                        .build(),
                TeacherAnnouncementResponse.builder()
                        .id(2L)
                        .title("Faculty meeting rescheduled")
                        .body("The weekly faculty meeting has moved to 4:00 PM in the staff room.")
                        .createdAt(now.minusHours(20))
                        .build(),
                TeacherAnnouncementResponse.builder()
                        .id(3L)
                        .title("Lab equipment maintenance")
                        .body("Lab 3 will be unavailable this Thursday morning for maintenance.")
                        .createdAt(now.minusDays(2))
                        .build()
        );
    }
}
