package com.collegeerp.Backend.announcement.dto;

import com.collegeerp.Backend.announcement.entity.Announcement.AudienceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnnouncementCreateRequest {
    @NotBlank
    @Size(max = 180)
    private String title;

    @NotBlank
    private String message;

    @NotNull
    private AudienceType audienceType;

    private Long audienceId;
}
