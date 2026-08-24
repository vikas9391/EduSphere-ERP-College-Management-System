package com.collegeerp.Backend.announcement.repository;

import com.collegeerp.Backend.announcement.entity.AnnouncementRecipient;
import com.collegeerp.Backend.announcement.entity.AnnouncementRecipient.RecipientType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface AnnouncementRecipientReadRepository extends Repository<AnnouncementRecipient, Long> {
    @Query("""
           SELECT r FROM AnnouncementRecipient r
           JOIN FETCH r.announcement a
           JOIN FETCH a.sender
           WHERE r.recipientType = :type AND r.recipientId = :recipientId
           ORDER BY a.createdAt DESC
           """)
    List<AnnouncementRecipient> findReceived(RecipientType type, Long recipientId);
}
