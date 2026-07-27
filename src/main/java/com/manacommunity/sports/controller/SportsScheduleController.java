package com.manacommunity.sports.controller;

import com.manacommunity.sports.dto.schedule.SportsScheduleResponse.EventListItem;
import com.manacommunity.sports.dto.schedule.SportsScheduleResponse.RegistrationListItem;
import com.manacommunity.sports.service.SportsScheduleService;
import com.manacommunity.sports.user.model.AppUser;
import com.manacommunity.sports.user.security.UserPrincipal;
import com.manacommunity.sports.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lean schedule endpoints that return only the fields the UI renders in list views.
 * Replaces the heavyweight SportsEventResponse (43+ fields) with EventListItem (~14 fields).
 */
@RestController
@RequestMapping("/api/sports/schedule")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SportsScheduleController {

    private final SportsScheduleService scheduleService;
    private final LoggedInUserService loggedInUserService;

    /** GET /api/sports/schedule/events/open?communityId= — lean event list for schedule page */
    @GetMapping("/events/open")
    public ResponseEntity<List<EventListItem>> getOpenEvents(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long communityId) {
        return ResponseEntity.ok(scheduleService.getOpenEvents(communityId));
    }

    /** GET /api/sports/schedule/events/mine — lean list of events created by the user */
    @GetMapping("/events/mine")
    public ResponseEntity<List<EventListItem>> getMyEvents(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(scheduleService.getMyEvents(user.getId()));
    }

    /** GET /api/sports/schedule/events/open-all — lean list of all open events */
    @GetMapping("/events/open-all")
    public ResponseEntity<List<EventListItem>> getAllOpenEvents(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(scheduleService.getAllOpenEvents());
    }

    /** GET /api/sports/schedule/registrations/mine — lean list of user's registrations */
    @GetMapping("/registrations/mine")
    public ResponseEntity<List<RegistrationListItem>> getMyRegistrations(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(scheduleService.getMyRegistrations(user.getId()));
    }
}
