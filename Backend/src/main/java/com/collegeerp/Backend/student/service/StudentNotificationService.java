package com.collegeerp.Backend.student.service;

import com.collegeerp.Backend.announcement.entity.AnnouncementRecipient;
import com.collegeerp.Backend.announcement.entity.AnnouncementRecipient.RecipientType;
import com.collegeerp.Backend.announcement.repository.AnnouncementRecipientReadRepository;
import com.collegeerp.Backend.announcement.repository.AnnouncementRecipientRepository;
import com.collegeerp.Backend.student.dto.NotificationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Student dashboard notification adapter backed by the persisted announcement recipient records.
 * Announcements are currently the first persisted notification source; the service is kept as a
 * small adapter so assignment, exam, result and attendance notifications can be added later
 * without changing the dashboard contract.
 */
@Service
public class StudentNotificationService {

    private final AnnouncementRecipientReadRepository recipientReadRepository;
    private final AnnouncementRecipientRepository recipientRepository;

    public StudentNotificationService(
            AnnouncementRecipientReadRepository recipientReadRepository,
            AnnouncementRecipientRepository recipientRepository) {
        this.recipientReadRepository = recipientReadRepository;
        this.recipientRepository = recipientRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long studentId) {
        return recipientReadRepository.findReceived(RecipientType.STUDENT, studentId).stream()
                .map(this::toNotification)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long studentId) {
        return recipientRepository.countUnread(RecipientType.STUDENT, studentId);
    }

    private NotificationResponse toNotification(AnnouncementRecipient recipient) {
        var announcement = recipient.getAnnouncement();
        return NotificationResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .message(announcement.getMessage())
                .type("ANNOUNCEMENT")
                .read(recipient.getReadAt() != null)
                .createdAt(announcement.getCreatedAt())
                .build();
    }
}
