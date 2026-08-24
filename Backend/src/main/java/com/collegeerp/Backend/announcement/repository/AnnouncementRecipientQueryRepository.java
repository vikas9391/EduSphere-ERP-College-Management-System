package com.collegeerp.Backend.announcement.repository;

import com.collegeerp.Backend.announcement.entity.AnnouncementRecipient.RecipientType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface AnnouncementRecipientQueryRepository extends Repository<com.collegeerp.Backend.announcement.entity.AnnouncementRecipient, Long> {

    @Query("SELECT r.announcement.id FROM AnnouncementRecipient r WHERE r.recipientType = :type AND r.recipientId = :recipientId")
    List<Long> findAnnouncementIds(RecipientType type, Long recipientId);
}
