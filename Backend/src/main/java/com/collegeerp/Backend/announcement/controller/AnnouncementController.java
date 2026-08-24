package com.collegeerp.Backend.announcement.controller;

import com.collegeerp.Backend.announcement.dto.*;
import com.collegeerp.Backend.announcement.service.AnnouncementService;
import com.collegeerp.Backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementController.class);

    private final AnnouncementService service;

    public AnnouncementController(AnnouncementService service) { this.service = service; }

    @GetMapping
    public List<AnnouncementResponse> getReceived(Authentication authentication) {
        UserPrincipal principal = principal(authentication);
        log.info("GET /api/announcements user={} role={} email={}", principal.getId(), principal.getRole(), principal.getEmail());
        List<AnnouncementResponse> result = service.received(principal);
        log.info("GET /api/announcements returned {} announcement(s) for user={} role={}", result.size(), principal.getId(), principal.getRole());
        return result;
    }

    @GetMapping("/unread-count")
    public long unreadCount(Authentication authentication) {
        UserPrincipal principal = principal(authentication);
        long count = service.unreadCount(principal);
        log.info("GET /api/announcements/unread-count user={} role={} count={}", principal.getId(), principal.getRole(), count);
        return count;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementResponse create(@Valid @RequestBody AnnouncementCreateRequest request, Authentication authentication) {
        UserPrincipal principal = principal(authentication);
        log.info("POST /api/announcements user={} role={} audienceType={} audienceId={} titleLength={} messageLength={}",
                principal.getId(), principal.getRole(), request.getAudienceType(), request.getAudienceId(),
                request.getTitle() == null ? 0 : request.getTitle().length(),
                request.getMessage() == null ? 0 : request.getMessage().length());
        AnnouncementResponse result = service.create(principal, request);
        log.info("POST /api/announcements created announcement id={} user={}", result.getId(), principal.getId());
        return result;
    }

    @GetMapping("/options")
    public List<AnnouncementAudienceOption> options(Authentication authentication) {
        UserPrincipal principal = principal(authentication);
        List<AnnouncementAudienceOption> result = service.audienceOptions(principal);
        log.info("GET /api/announcements/options user={} role={} returned {} option(s)", principal.getId(), principal.getRole(), result.size());
        return result;
    }

    @GetMapping("/contacts")
    public List<AnnouncementContact> contacts(Authentication authentication) {
        UserPrincipal principal = principal(authentication);
        List<AnnouncementContact> result = service.contacts(principal);
        log.info("GET /api/announcements/contacts user={} role={} returned {} contact(s)", principal.getId(), principal.getRole(), result.size());
        return result;
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id, Authentication authentication) {
        UserPrincipal principal = principal(authentication);
        log.info("POST /api/announcements/{}/read user={} role={}", id, principal.getId(), principal.getRole());
        service.markRead(principal, id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(Authentication authentication) {
        UserPrincipal principal = principal(authentication);
        log.info("POST /api/announcements/read-all user={} role={}", principal.getId(), principal.getRole());
        service.markAllRead(principal);
    }

    private UserPrincipal principal(Authentication authentication) {
        return (UserPrincipal) authentication.getPrincipal();
    }
}
