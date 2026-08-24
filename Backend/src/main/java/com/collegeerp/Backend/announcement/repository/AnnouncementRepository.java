package com.collegeerp.Backend.announcement.repository;

import com.collegeerp.Backend.announcement.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("""
           SELECT a FROM Announcement a
           JOIN FETCH a.sender
           WHERE EXISTS (
               SELECT r.id FROM AnnouncementRecipient r
               WHERE r.announcement.id = a.id
                 AND r.recipientType = :recipientType
                 AND r.recipientId = :recipientId
           )
           ORDER BY a.createdAt DESC
           """)
    List<Announcement> findReceived(AnnouncementRecipientType recipientType, Long recipientId);

    enum AnnouncementRecipientType { USER, STUDENT }
}
