package com.manacommunity.sports.controller;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.dto.AuctionTeamRequest;
import com.manacommunity.sports.dto.AuctionTeamResponse;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.sports.model.AuctionTeam;
import com.manacommunity.common.user.security.UserPrincipal;
import com.manacommunity.sports.service.AuctionTeamService;
import com.manacommunity.sports.user.service.LoggedInUserService;
import com.manacommunity.sports.service.PermissionCheckService;
import static com.manacommunity.sports.constants.PermissionConstants.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auction/teams")
@RequiredArgsConstructor
public class AuctionTeamController {

    private final AuctionTeamService auctionTeamService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping("/{configId}")
    public ResponseEntity<List<AuctionTeamResponse>> getTeams(
            @PathVariable Long configId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_TEAMS_DASHBOARD, VIEW_PLAYER_POOL);
        return ResponseEntity.ok(auctionTeamService.getTeams(configId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/nominated/{eventId}")
    public ResponseEntity<List<AuctionTeamResponse>> getNominatedCaptains(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_TEAMS_DASHBOARD, VIEW_PLAYER_POOL);
        return ResponseEntity.ok(auctionTeamService.getNominatedCaptains(eventId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/captain/mine")
    public ResponseEntity<List<AuctionTeamResponse>> getMyCaptainRegistrations(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_PLAYER_POOL);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionTeamService.getCaptainRegistration(loggedInUser.getId()).stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<AuctionTeamResponse> createTeam(
            @Valid @RequestBody AuctionTeamRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_TEAMS_DASHBOARD);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        AuctionTeam created = auctionTeamService.createTeam(req, loggedInUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{teamId}/confirm-captain")
    public ResponseEntity<AuctionTeamResponse> confirmCaptain(
            @PathVariable Long teamId,
            @RequestParam boolean confirm,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        boolean isAdmin = permissionCheckService.hasAnyPermission(principal, CREATE_EDIT_TEAMS_DASHBOARD);
        return ResponseEntity.ok(toResponse(auctionTeamService.confirmCaptain(teamId, confirm, loggedInUser.getId(), isAdmin)));
    }

    @PostMapping("/nominate")
    public ResponseEntity<AuctionTeamResponse> nominateCaptain(
            @RequestParam Long eventId,
            @RequestParam boolean nominate,
            @RequestParam(required = false) String teamName,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(toResponse(auctionTeamService.nominateCaptain(eventId, loggedInUser.getId(), nominate, teamName)));
    }

    @GetMapping("/captain/all")
    public ResponseEntity<List<AuctionTeamResponse>> getAllCaptainNominations(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_PLAYER_POOL);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionTeamService.getCaptainRegistration(loggedInUser.getId()).stream().map(this::toResponse).toList());
    }

    private AuctionTeamResponse toResponse(AuctionTeam t) {
        return AuctionTeamResponse.builder()
                .id(t.getId())
                .configId(t.getConfig() != null ? t.getConfig().getId() : null)
                .eventId(t.getEventId())
                .teamName(t.getTeamName())
                .ownerName(t.getOwnerName())
                .ownerUserId(t.getOwnerUser() != null ? t.getOwnerUser().getId() : null)
                .captainUserId(t.getCaptainUser() != null ? t.getCaptainUser().getId() : null)
                .colorHex(t.getColorHex())
                .totalBudget(t.getTotalBudget())
                .remainingBudget(t.getRemainingBudget())
                .spent(t.getSpent())
                .captainNomination(t.getCaptainNomination())
                .captainConfirmation(t.getCaptainConfirmation())
                .createdAt(t.getCreatedAt())
                .build();
    }
}


