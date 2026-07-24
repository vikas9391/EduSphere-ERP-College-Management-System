package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.student.dto.NotificationResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <b>PLACEHOLDER / MOCK DATA.</b>
 * <p>
 * There is no Notification entity, table, or module anywhere in this codebase. Per the task
 * instructions ("If Notification module does not exist: create a placeholder service with mock
 * data and document it clearly"), this returns a small, fixed, in-memory list rather than
 * reading from any database. Nothing here is persisted or student-specific - every student
 * currently sees the same mock notifications. Replace this class's body with a real
 * repository-backed query once a Notification entity/table is introduced.
 */
@Service
public class StudentNotificationService {

    public List<NotificationResponse> getNotifications(Long studentId) {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                NotificationResponse.builder()
                        .id(1L)
                        .title("Welcome to EduSphere")
                        .message("Your student portal is ready to use.")
                        .type("INFO")
                        .read(false)
                        .createdAt(now.minusDays(1))
                        .build(),
                NotificationResponse.builder()
                        .id(2L)
                        .title("Check your assignments")
                        .message("You may have assignments due soon - review the Assignments tab.")
                        .type("REMINDER")
                        .read(false)
                        .createdAt(now.minusHours(6))
                        .build(),
                NotificationResponse.builder()
                        .id(3L)
                        .title("Results published")
                        .message("Check the Results tab for any newly published semester results.")
                        .type("RESULT")
                        .read(true)
                        .createdAt(now.minusDays(3))
                        .build()
        );
    }

    public long getUnreadCount(Long studentId) {
        return getNotifications(studentId).stream().filter(n -> !n.isRead()).count();
    }
}
