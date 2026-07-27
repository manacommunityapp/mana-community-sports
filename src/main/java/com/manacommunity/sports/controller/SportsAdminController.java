package com.manacommunity.sports.controller;

import com.manacommunity.sports.dto.dashboard.SportsAdminFormDataResponse;
import com.manacommunity.sports.dto.dashboard.SportsAdminOverviewResponse;
import com.manacommunity.sports.user.model.AppUser;
import com.manacommunity.sports.user.security.UserPrincipal;
import com.manacommunity.sports.user.service.LoggedInUserService;
import com.manacommunity.sports.service.PermissionCheckService;
import com.manacommunity.sports.service.SportsAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.manacommunity.sports.constants.PermissionConstants.VIEW_SPORTS_MAIN;

/**
 * Lean aggregation endpoints for the Sports Admin page:
 *
 * <ul>
 *   <li>{@code GET /api/sports/admin/overview}  — tournaments + events lists (Dashboard / Sports Event tabs)</li>
 *   <li>{@code GET /api/sports/admin/form-data} — sports + categories + communities (Create forms)</li>
 * </ul>
 *
 * Each replaces several full-entity fetches with one trimmed response.
 */
@RestController
@RequestMapping("/api/sports/admin")
@RequiredArgsConstructor
public class SportsAdminController {

    private final SportsAdminService adminService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping("/overview")
    public ResponseEntity<SportsAdminOverviewResponse> overview(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(adminService.getOverview(user, communityId));
    }

    @GetMapping("/form-data")
    public ResponseEntity<SportsAdminFormDataResponse> formData(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(adminService.getFormData(user));
    }
}
