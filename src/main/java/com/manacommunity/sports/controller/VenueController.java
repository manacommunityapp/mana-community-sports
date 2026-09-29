package com.manacommunity.sports.controller;

import com.manacommunity.common.enums.*;
import static com.manacommunity.sports.constants.PermissionConstants.*;

import com.manacommunity.sports.constants.PermissionConstants;
import com.manacommunity.sports.dto.VenueRequest;
import com.manacommunity.sports.dto.VenueResponse;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.sports.model.Venue;
import com.manacommunity.common.user.security.UserPrincipal;
import com.manacommunity.sports.user.service.LoggedInUserService;
import com.manacommunity.sports.service.PermissionCheckService;
import com.manacommunity.sports.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping
    public ResponseEntity<List<VenueResponse>> getVenues(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        boolean isSuperAdmin = ROLE_SUPER_ADMIN.equals(loggedInUser.getRole());
        Long targetCommunityId = communityId;
        if (!isSuperAdmin) {
            targetCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
            if (targetCommunityId == null) {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }
        }
        if (targetCommunityId == null) {
            return ResponseEntity.ok(venueService.getAllVenues());
        }
        return ResponseEntity.ok(venueService.getVenuesByCommunityId(targetCommunityId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenueResponse> getVenueById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(venueService.getVenueResponseById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','SPORTS_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<VenueResponse> createVenue(
            @RequestParam(required = false) Long communityId,
            @Valid @RequestBody VenueRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long targetCommunityId = communityId;
        if (!ROLE_SUPER_ADMIN.equals(loggedInUser.getRole())) {
            targetCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        }
        return ResponseEntity.ok(venueService.createVenue(targetCommunityId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VenueResponse> updateVenue(
            @PathVariable Long id,
            @Valid @RequestBody VenueRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);

        Venue existing = venueService.getVenueById(id);
        boolean timingChanged = !Objects.equals(existing.getOpeningTime(), request.getOpeningTime())
                || !Objects.equals(existing.getClosingTime(), request.getClosingTime());
        if (timingChanged) {
            permissionCheckService.requireAnyPermission(principal, PermissionConstants.EDIT_VENUE_TIMING);
        }

        return ResponseEntity.ok(venueService.updateVenue(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<Void> deleteVenue(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        venueService.deleteVenue(id);
        return ResponseEntity.noContent().build();
    }
}


