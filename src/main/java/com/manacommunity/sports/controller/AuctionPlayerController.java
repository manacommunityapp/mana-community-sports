package com.manacommunity.sports.controller;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.dto.AuctionPlayerRequest;
import com.manacommunity.sports.dto.AuctionPlayerResponse;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.sports.model.AuctionPlayer;
import com.manacommunity.sports.model.AuctionConfig;
import com.manacommunity.common.user.security.UserPrincipal;
import com.manacommunity.sports.service.AuctionPlayerService;
import com.manacommunity.sports.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auction/players")
@RequiredArgsConstructor
public class AuctionPlayerController {

    private final AuctionPlayerService auctionPlayerService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AUCTION_ADMIN','SPORTS_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<AuctionPlayerResponse> createPlayer(
            @Valid @RequestBody AuctionPlayerRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        AuctionPlayer player = AuctionPlayer.builder()
                .config(AuctionConfig.builder().id(req.getConfigId()).build())
                .user(AppUser.builder().id(req.getUserId()).build())
                .playerName(req.getPlayerName())
                .category(req.getCategory())
                .playerRole(req.getPlayerRole())
                .age(req.getAge())
                .basePrice(req.getBasePrice())
                .status(AuctionPlayer.PlayerStatus.QUEUED)
                .build();
        AuctionPlayer saved = auctionPlayerService.savePlayer(player);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    static AuctionPlayerResponse toResponse(AuctionPlayer p) {
        return AuctionPlayerResponse.builder()
                .id(p.getId())
                .configId(p.getConfig() != null ? p.getConfig().getId() : null)
                .userId(p.getUser() != null ? p.getUser().getId() : null)
                .playerName(p.getPlayerName())
                .category(p.getCategory())
                .playerRole(p.getPlayerRole())
                .age(p.getAge())
                .basePrice(p.getBasePrice())
                .statsJson(p.getStatsJson())
                .queueOrder(p.getQueueOrder())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .assignedTeamId(p.getAssignedTeam() != null ? p.getAssignedTeam().getId() : null)
                .assignedTeamName(p.getAssignedTeam() != null ? p.getAssignedTeam().getTeamName() : null)
                .soldPrice(p.getSoldPrice())
                .rtmUsed(p.getRtmUsed())
                .soldAt(p.getSoldAt())
                .build();
    }
}


