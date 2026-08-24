package com.collegeerp.Backend.announcement.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AnnouncementContact {
    Long id;
    String name;
    String email;
    String phone;
    String role;
}
