package com.manacommunity.sports.model.scheduler;

/**
 * Outcome status of a schedule generation/save operation.
 */
public enum GenerationStatus {
    SUCCESS,
    FAILED,
    PARTIAL   // some matches saved but validation warnings exist
}
