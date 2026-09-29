package com.manacommunity.sports.dto;

import com.manacommunity.common.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AuctionPlayerRequest {

    @NotBlank
    private String playerName;
    private Long configId;
    private Long userId;
    @NotBlank
    private String category;

    private String playerRole;

    @Min(1)
    private Integer age;

    @NotNull
    @Min(0)
    private Integer basePrice;

    // Optional stats mapping
    private Integer matches;
    private Integer runs;
    private Integer wickets;
    private Double strikeRate;
    private Double economy;
    private Double avgScore;
}

