package com.collegeerp.Backend.teacher.service;

import com.collegeerp.Backend.announcement.service.AnnouncementService;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.teacher.dto.TeacherAnnouncementResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Teacher dashboard announcement reader backed by the persisted announcement module. */
@Service
@Transactional(readOnly = true)
public class TeacherAnnouncementService {

    private final AnnouncementService announcementService;

    public TeacherAnnouncementService(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    public List<TeacherAnnouncementResponse> getAnnouncements(UserPrincipal principal) {
        return announcementService.received(principal).stream()
                .limit(5)
                .map(a -> TeacherAnnouncementResponse.builder()
                        .id(a.getId())
                        .title(a.getTitle())
                        .body(a.getMessage())
                        .createdAt(a.getCreatedAt())
                        .build())
                .toList();
    }
}
