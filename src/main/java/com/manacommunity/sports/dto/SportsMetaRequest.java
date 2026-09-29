package com.manacommunity.sports.dto;

import com.manacommunity.common.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SportsMetaRequest(
    @NotBlank @Size(max = 50) String name,
    String icon,
    String iconUrl,
    List<String> formats,
    Boolean active
) {}

