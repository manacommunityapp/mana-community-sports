package com.manacommunity.sports.controller;

import com.manacommunity.sports.dto.*;
import com.manacommunity.sports.user.model.AppUser;
import com.manacommunity.sports.model.AuctionBid;
import com.manacommunity.sports.model.AuctionPlayer;
import com.manacommunity.sports.model.AuctionTeam;
import com.manacommunity.sports.user.security.UserPrincipal;
import com.manacommunity.sports.service.AuctionService;
import com.manacommunity.sports.service.AuctionTeamService;
import com.manacommunity.sports.user.service.LoggedInUserService;
import com.manacommunity.sports.service.PermissionCheckService;
import static com.manacommunity.sports.constants.PermissionConstants.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/auction/live")
@RequiredArgsConstructor
public class AuctionLiveController {

    private final AuctionService auctionService;
    private final AuctionTeamService auctionTeamService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping("/{configId}/current-player")
    public ResponseEntity<PlayerWithBidResponse> getCurrentPlayer(
            @PathVariable Long configId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.getCurrentPlayer(configId));
    }

    @GetMapping("/{configId}/random-player")
    public ResponseEntity<PlayerWithBidResponse> pickRandomPlayer(
            @PathVariable Long configId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.pickRandomPlayer(configId));
    }

    @PostMapping("/bid")
    public ResponseEntity<AuctionBidResponse> placeBid(
            @Valid @RequestBody BidRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toBidResponse(auctionService.placeBid(req, loggedInUser.getId())));
    }

    @PostMapping("/sold")
    public ResponseEntity<AuctionPlayerResponse> soldPlayer(
            @Valid @RequestBody SoldPlayerRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        AuctionPlayer player = auctionService.soldPlayer(req, loggedInUser.getId());
        return ResponseEntity.ok(AuctionPlayerController.toResponse(player));
    }

    @PostMapping("/{playerId}/pass")
    public ResponseEntity<AuctionPlayerResponse> passPlayer(
            @PathVariable Long playerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        AuctionPlayer player = auctionService.passPlayer(playerId, loggedInUser.getId());
        return ResponseEntity.ok(AuctionPlayerController.toResponse(player));
    }

    @GetMapping("/bids/{playerId}")
    public ResponseEntity<List<AuctionBidResponse>> getBidHistory(
            @PathVariable Long playerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.getBidHistory(playerId).stream()
                .map(this::toBidResponse).toList());
    }

    @GetMapping("/{configId}/teams")
    public ResponseEntity<List<AuctionTeamResponse>> getTeams(
            @PathVariable Long configId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionTeamService.getTeams(configId).stream()
                .map(this::toTeamResponse).toList());
    }

    @GetMapping("/{configId}/players")
    public ResponseEntity<List<AuctionPlayerResponse>> getPlayers(
            @PathVariable Long configId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.getPlayers(configId, category, status).stream()
                .map(AuctionPlayerController::toResponse).toList());
    }

    private AuctionBidResponse toBidResponse(AuctionBid b) {
        return AuctionBidResponse.builder()
                .id(b.getId())
                .configId(b.getConfig() != null ? b.getConfig().getId() : null)
                .playerId(b.getPlayer() != null ? b.getPlayer().getId() : null)
                .teamId(b.getTeam() != null ? b.getTeam().getId() : null)
                .teamName(b.getTeam() != null ? b.getTeam().getTeamName() : null)
                .bidAmount(b.getBidAmount())
                .incrementUsed(b.getIncrementUsed())
                .isRtm(b.getIsRtm())
                .bidByUserId(b.getBidByUser() != null ? b.getBidByUser().getId() : null)
                .bidAt(b.getBidAt())
                .build();
    }

    private AuctionTeamResponse toTeamResponse(AuctionTeam t) {
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
