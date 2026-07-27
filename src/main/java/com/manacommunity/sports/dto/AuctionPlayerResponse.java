package com.manacommunity.sports.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuctionPlayerResponse {
    private Long id;
    private Long configId;
    private Long userId;
    private String playerName;
    private String category;
    private String playerRole;
    private Integer age;
    private Integer basePrice;
    private String statsJson;
    private Integer queueOrder;
    private String status;
    private Long assignedTeamId;
    private String assignedTeamName;
    private Long soldPrice;
    private Boolean rtmUsed;
    private LocalDateTime soldAt;
}
