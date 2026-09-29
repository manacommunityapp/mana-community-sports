package com.manacommunity.sports.dto;

import com.manacommunity.common.enums.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuctionTeamResponse {
    private Long id;
    private Long configId;
    private Long eventId;
    private String teamName;
    private String ownerName;
    private Long ownerUserId;
    private Long captainUserId;
    private String colorHex;
    private Long totalBudget;
    private Long remainingBudget;
    private Long spent;
    private Boolean captainNomination;
    private Boolean captainConfirmation;
    private LocalDateTime createdAt;
}

