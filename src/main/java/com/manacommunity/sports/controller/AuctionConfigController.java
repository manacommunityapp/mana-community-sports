package com.manacommunity.sports.controller;

import com.manacommunity.sports.dto.AuctionConfigRequest;
import com.manacommunity.sports.dto.AuctionConfigResponse;
import com.manacommunity.sports.user.model.AppUser;
import com.manacommunity.sports.model.AuctionConfig;
import com.manacommunity.sports.user.security.UserPrincipal;
import com.manacommunity.sports.service.AuctionCsvService;
import com.manacommunity.sports.service.AuctionService;
import com.manacommunity.sports.user.service.LoggedInUserService;
import com.manacommunity.sports.service.PermissionCheckService;
import static com.manacommunity.sports.constants.PermissionConstants.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auction/config")
@RequiredArgsConstructor
public class AuctionConfigController {

    private final AuctionService     auctionService;
    private final AuctionCsvService  csvService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping
    public ResponseEntity<List<AuctionConfigResponse>> getConfigs(
            @RequestParam Long sportId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_AUCTION_CONFIG, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        return ResponseEntity.ok(auctionService.getConfigResponsesBySportAndCommunity(sportId, communityId));
    }

    /** GET all configs for the user's community across all sports */
    @GetMapping("/all")
    public ResponseEntity<List<AuctionConfigResponse>> getCommunityConfigs(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_AUCTION_CONFIG, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        return ResponseEntity.ok(auctionService.getConfigResponsesByCommunity(communityId));
    }

    /** GET check if auction config exists for the logged-in user's community */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkConfigExists(
            @RequestParam Long sportId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_AUCTION_CONFIG, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        List<AuctionConfig> configs = auctionService.getConfigsBySportAndCommunity(sportId, communityId);
        boolean exists = !configs.isEmpty();
        return ResponseEntity.ok(Map.of(
                "configExists", exists,
                "configCount", configs.size(),
                "communityId", communityId != null ? communityId : 0
        ));
    }

    /** GET single config by ID */
    @GetMapping("/{id}")
    public ResponseEntity<AuctionConfigResponse> getConfig(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_AUCTION_CONFIG, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.getConfigResponse(id));
    }

    /** GET auction stats by config ID */
    @GetMapping("/{id}/stats")
    public ResponseEntity<com.manacommunity.sports.dto.AuctionStatsResponse> getStats(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_AUCTION_CONFIG, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.getAuctionStats(id));
    }

    /** POST create new auction config (admin only) */
    @PostMapping
    public ResponseEntity<AuctionConfigResponse> createConfig(
            @Valid @RequestBody AuctionConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_AUCTION_CONFIG);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        AuctionConfig created = auctionService.createConfig(req, loggedInUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(auctionService.getConfigResponse(created.getId()));
    }

    /** PUT update auction rules dynamically — cannot update when LIVE */
    @PutMapping("/{id}")
    public ResponseEntity<AuctionConfigResponse> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody AuctionConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_AUCTION_CONFIG);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        auctionService.updateConfig(id, req);
        return ResponseEntity.ok(auctionService.getConfigResponse(id));
    }

    /** PUT change auction status: DRAFT→ACTIVE→LIVE→COMPLETED */
    @PutMapping("/{id}/status")
    public ResponseEntity<AuctionConfigResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        auctionService.updateStatus(id, status);
        return ResponseEntity.ok(auctionService.getConfigResponse(id));
    }

    /** POST upload players via CSV */
    @PostMapping("/{id}/players/upload")
    public ResponseEntity<AuctionCsvService.UploadResult> uploadPlayers(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(csvService.uploadPlayersFromFile(id, file));
    }

    /** POST create single player manually */
    @PostMapping("/{id}/players")
    public ResponseEntity<com.manacommunity.sports.dto.AuctionPlayerResponse> createPlayer(
            @PathVariable Long id,
            @Valid @RequestBody com.manacommunity.sports.dto.AuctionPlayerRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(AuctionPlayerController.toResponse(auctionService.createPlayer(id, req)));
    }

    /** GET /api/auction/config/{id}/registration-count — get confirmed registration count */
    @GetMapping("/{id}/registration-count")
    public ResponseEntity<Long> getRegistrationCount(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_AUCTION_CONFIG, VIEW_LIVE_AUCTION);
        return ResponseEntity.ok(auctionService.getConfirmedRegistrationCount(id));
    }
}
