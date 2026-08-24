package com.collegeerp.Backend.announcement.dto;

import com.collegeerp.Backend.announcement.entity.Announcement.AudienceType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class AnnouncementResponse {
    Long id;
    String title;
    String message;
    AudienceType audienceType;
    Long audienceId;
    String senderName;
    LocalDateTime createdAt;
    boolean read;
}
