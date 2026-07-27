package com.manacommunity.sports.controller;

import com.manacommunity.sports.dto.SportsScheduleStatsResponse;
import com.manacommunity.sports.service.scheduler.SportsScheduleAllEventsService;
import com.manacommunity.sports.user.model.AppUser;
import com.manacommunity.sports.user.security.UserPrincipal;
import com.manacommunity.sports.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sports/schedule/stats")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SportsScheduleAllEventsController {

    private final SportsScheduleAllEventsService scheduleService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<SportsScheduleStatsResponse> getStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(scheduleService.getStats(user));
    }

    @GetMapping("/total")
    public ResponseEntity<Long> getTotalGames(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(scheduleService.getTotalGames(user));
    }

    @GetMapping("/live")
    public ResponseEntity<Long> getLiveGames(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(scheduleService.getLiveGames(user));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<Long> getUpcomingGames(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(scheduleService.getUpcomingGames(user));
    }

    @GetMapping("/completed")
    public ResponseEntity<Long> getCompletedGames(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(scheduleService.getCompletedGames(user));
    }
}
