package com.collegeerp.Backend.announcement.controller;

import com.collegeerp.Backend.announcement.dto.*;
import com.collegeerp.Backend.announcement.service.AnnouncementService;
import com.collegeerp.Backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService service;

    public AnnouncementController(AnnouncementService service) { this.service = service; }

    @GetMapping
    public List<AnnouncementResponse> getReceived(Authentication authentication) {
        return service.received(principal(authentication));
    }

    @GetMapping("/unread-count")
    public long unreadCount(Authentication authentication) {
        return service.unreadCount(principal(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementResponse create(@Valid @RequestBody AnnouncementCreateRequest request, Authentication authentication) {
        return service.create(principal(authentication), request);
    }

    @GetMapping("/options")
    public List<AnnouncementAudienceOption> options(Authentication authentication) {
        return service.audienceOptions(principal(authentication));
    }

    @GetMapping("/contacts")
    public List<AnnouncementContact> contacts(Authentication authentication) {
        return service.contacts(principal(authentication));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id, Authentication authentication) {
        service.markRead(principal(authentication), id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(Authentication authentication) {
        service.markAllRead(principal(authentication));
    }

    private UserPrincipal principal(Authentication authentication) {
        return (UserPrincipal) authentication.getPrincipal();
    }
}
