package com.manacommunity.sports.dto.dashboard;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.response.CommunityResponse;

import java.util.List;

/**
 * Reference data for the Sports Admin "Create Tournament" / "Create Venue" forms.
 * Consolidates the three dropdown fetches (sports meta, player categories,
 * communities) into one call, each trimmed to the fields the form binds to.
 */
public record SportsAdminFormDataResponse(
        List<SportOption> sports,
        List<CategoryOption> categories,
        List<CommunityResponse> communities
) {

    public record SportOption(
            Long id,
            String name,
            String icon,
            String iconUrl,
            List<String> formats,
            Long communityId
    ) {}

    public record CategoryOption(
            Long id,
            String name,
            String type,
            String categoryType,
            String description,
            Integer minAge,
            Integer maxAge,
            String gender
    ) {}
}

