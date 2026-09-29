package com.manacommunity.sports.dto;

import com.manacommunity.common.enums.*;
import java.util.List;

public record SportsMetaResponse(
    Long id,
    String name,
    String icon,
    String iconUrl,
    Long communityId,
    List<String> formats,
    Boolean active
) {}

