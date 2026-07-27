package com.manacommunity.sports.controller;

import com.manacommunity.sports.dto.NotificationResponse;
import com.manacommunity.sports.dto.dashboard.SportsDashboardResponse.MyRegistration;
import com.manacommunity.sports.dto.dashboard.SportsDashboardResponse.Stats;
import com.manacommunity.sports.dto.dashboard.SportsDashboardResponse.TournamentCard;
import com.manacommunity.sports.dto.dashboard.SportsDashboardResponse.UpcomingEvent;
import com.manacommunity.sports.service.NotificationManagementService;
import com.manacommunity.sports.service.PermissionCheckService;
import com.manacommunity.sports.service.SportsDashboardService;
import com.manacommunity.sports.user.model.AppUser;
import com.manacommunity.sports.user.security.UserPrincipal;
import com.manacommunity.sports.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.manacommunity.sports.constants.PermissionConstants.VIEW_SPORTS_MAIN;

@RestController
@RequestMapping("/api/sports/dashboard")
@RequiredArgsConstructor
public class SportsDashboardController {

    private final SportsDashboardService dashboardService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;
    private final NotificationManagementService notificationService;

    @GetMapping("/stats")
    public ResponseEntity<Stats> getStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dashboardService.getStats(user));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<UpcomingEvent>> getUpcomingEvents(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dashboardService.getUpcomingEvents(user));
    }

    @GetMapping("/open-tournaments")
    public ResponseEntity<List<TournamentCard>> getOpenTournaments(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dashboardService.getOpenTournaments(user));
    }

    @GetMapping("/closed-tournaments")
    public ResponseEntity<List<TournamentCard>> getClosedTournaments(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dashboardService.getClosedTournaments(user));
    }

    @GetMapping("/my-registrations")
    public ResponseEntity<List<MyRegistration>> getMyRegistrations(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dashboardService.getMyRegistrations(user));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationResponse>> getDashboardNotifications(
            @AuthenticationPrincipal UserPrincipal principal) {
        // Fetch top 4 active notifications for the logged-in user
        Page<NotificationResponse> page = notificationService.getUserNotifications(principal.getId(), 0, 4);
        return ResponseEntity.ok(page.getContent());
    }
}
