package com.manacommunity.sports.model.scheduler;

/**
 * Actions tracked in the schedule_generation_log table.
 */
public enum GenerationAction {
    GENERATE,           // full schedule generation from config
    SAVE_DRAFT,         // unified save with status=DRAFT
    SAVE_PUBLISH,       // unified save with status=PUBLISHED
    REGENERATE,         // re-generation after edits
    DELETE,             // match deletion
    PLAYOFF_GENERATE,   // stateless playoff bracket generation
    BULK_SAVE,          // legacy bulk match save
    STATUS_UPDATE       // status-only update (DRAFT→PUBLISHED)
}
