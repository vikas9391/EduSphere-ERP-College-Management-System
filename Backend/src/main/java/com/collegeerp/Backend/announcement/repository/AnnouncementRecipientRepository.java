package com.collegeerp.Backend.announcement.repository;

import com.collegeerp.Backend.announcement.entity.AnnouncementRecipient;
import com.collegeerp.Backend.announcement.entity.AnnouncementRecipient.RecipientType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AnnouncementRecipientRepository extends JpaRepository<AnnouncementRecipient, Long> {

    @Query("SELECT r FROM AnnouncementRecipient r WHERE r.announcement.id = :announcementId AND r.recipientType = :type AND r.recipientId = :recipientId")
    List<AnnouncementRecipient> findForRecipient(Long announcementId, RecipientType type, Long recipientId);

    @Modifying
    @Query("UPDATE AnnouncementRecipient r SET r.readAt = :readAt WHERE r.announcement.id = :announcementId AND r.recipientType = :type AND r.recipientId = :recipientId")
    int markRead(Long announcementId, RecipientType type, Long recipientId, LocalDateTime readAt);

    @Query("SELECT COUNT(r) FROM AnnouncementRecipient r WHERE r.recipientType = :type AND r.recipientId = :recipientId AND r.readAt IS NULL")
    long countUnread(RecipientType type, Long recipientId);

    @Modifying
    @Query("UPDATE AnnouncementRecipient r SET r.readAt = :readAt WHERE r.recipientType = :type AND r.recipientId = :recipientId AND r.readAt IS NULL")
    int markAllRead(RecipientType type, Long recipientId, LocalDateTime readAt);
}
