package com.manacommunity.sports.dto.scheduler;

import com.manacommunity.common.enums.*;
/**
 * Request body for PUT /api/tournament/{configId}/matches/status.
 * Updates the status of every match belonging to the config —
 * used by the "Save as Draft" (DRAFT) and "Save & Publish" (PUBLISHED) buttons.
 */
public record MatchStatusUpdateRequest(String status) {}
