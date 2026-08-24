package com.collegeerp.Backend.announcement.dto;

import com.collegeerp.Backend.announcement.entity.Announcement.AudienceType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AnnouncementAudienceOption {
    AudienceType type;
    Long id;
    String label;
}
