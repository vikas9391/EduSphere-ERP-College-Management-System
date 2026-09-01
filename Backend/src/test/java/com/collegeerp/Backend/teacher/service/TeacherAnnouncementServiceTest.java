package com.collegeerp.Backend.teacher.service;

import com.collegeerp.Backend.announcement.dto.AnnouncementResponse;
import com.collegeerp.Backend.announcement.service.AnnouncementService;
import com.collegeerp.Backend.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherAnnouncementServiceTest {

    @Mock
    private AnnouncementService announcementService;

    @Test
    void mapsReceivedPersistedAnnouncementsForTeacher() {
        UserPrincipal principal = new UserPrincipal(7L, "teacher@example.com", "TEACHER");
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 1, 9, 0);
        AnnouncementResponse received = AnnouncementResponse.builder()
                .id(91L)
                .title("Department meeting")
                .message("Meet in seminar hall")
                .createdAt(createdAt)
                .read(false)
                .build();

        when(announcementService.received(principal)).thenReturn(List.of(received));

        var response = new TeacherAnnouncementService(announcementService).getAnnouncements(principal);

        assertEquals(1, response.size());
        assertEquals(91L, response.get(0).getId());
        assertEquals("Department meeting", response.get(0).getTitle());
        assertEquals("Meet in seminar hall", response.get(0).getBody());
        assertEquals(createdAt, response.get(0).getCreatedAt());
        verify(announcementService).received(principal);
    }
}
