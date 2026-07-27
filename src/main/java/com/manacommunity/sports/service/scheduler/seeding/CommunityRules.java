package com.manacommunity.sports.service.scheduler.seeding;

import com.manacommunity.sports.model.Community;

import com.manacommunity.sports.dto.scheduler.PlayoffMatchDraftResponse.ParticipantRef;

/**
 * Community first-round pairing rules, keyed off the player's flat number:
 * <ul>
 *   <li><b>Rule 1</b> — two players in the <i>same flat</i> should not meet (e.g. {@code A302} vs {@code A302}).</li>
 *   <li><b>Rule 2</b> — two players in the <i>same tower</i> should not meet ({@code A302} vs {@code A305}; tower = {@code A}).</li>
 * </ul>
 * Applied only when community rules are enabled for the draw.
 */
public final class CommunityRules {

    private CommunityRules() {
    }

    /** Two players conflict when they share a flat (Rule 1) or a tower (Rule 2). */
    public static boolean conflict(ParticipantRef a, ParticipantRef b) {
        if (a == null || b == null) return false;
        String flatA = norm(a.flatNumber());
        String flatB = norm(b.flatNumber());
        if (!flatA.isEmpty() && flatA.equals(flatB)) return true;   // same flat
        String towerA = tower(a.flatNumber());
        String towerB = tower(b.flatNumber());
        return !towerA.isEmpty() && towerA.equals(towerB);          // same tower
    }

    /** Leading letters of a flat number (e.g. "A302" → "A", "A-305" → "A", "302" → ""). */
    public static String tower(String flat) {
        if (flat == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : flat.trim().toCharArray()) {
            if (Character.isLetter(c)) sb.append(Character.toUpperCase(c));
            else break;
        }
        return sb.toString();
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
