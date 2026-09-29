package com.manacommunity.sports.service.scheduler.seeding;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;

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
        return conflict(a, b, true, true);
    }

    public static boolean conflict(ParticipantRef a, ParticipantRef b, boolean checkFlat, boolean checkTower) {
        if (a == null || b == null) return false;
        if (checkFlat) {
            String flatA = norm(a.flatNumber());
            String flatB = norm(b.flatNumber());
            if (!flatA.isEmpty() && flatA.equals(flatB)) return true;   // same flat
        }
        if (checkTower) {
            String towerA = tower(a.flatNumber());
            String towerB = tower(b.flatNumber());
            if (!towerA.isEmpty() && towerA.equals(towerB)) return true; // same tower
        }
        return false;
    }

    /** Conflict check for two AuctionTeam entities based on captain / players flat and tower info. */
    public static boolean conflict(com.manacommunity.sports.model.AuctionTeam a,
                                  com.manacommunity.sports.model.AuctionTeam b,
                                  boolean checkFlat,
                                  boolean checkTower) {
        if (a == null || b == null) return false;
        java.util.Set<String> flatsA = extractFlats(a);
        java.util.Set<String> flatsB = extractFlats(b);

        if (checkFlat) {
            for (String fA : flatsA) {
                if (!fA.isEmpty() && flatsB.contains(fA)) return true;
            }
        }

        if (checkTower) {
            java.util.Set<String> towersA = flatsA.stream().map(CommunityRules::tower).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toSet());
            java.util.Set<String> towersB = flatsB.stream().map(CommunityRules::tower).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toSet());
            for (String tA : towersA) {
                if (towersB.contains(tA)) return true;
            }
        }
        return false;
    }

    private static java.util.Set<String> extractFlats(com.manacommunity.sports.model.AuctionTeam team) {
        java.util.Set<String> flats = new java.util.HashSet<>();
        if (team.getCaptainUser() != null && team.getCaptainUser().getFlatNo() != null) {
            flats.add(norm(team.getCaptainUser().getFlatNo()));
        }
        if (team.getOwnerUser() != null && team.getOwnerUser().getFlatNo() != null) {
            flats.add(norm(team.getOwnerUser().getFlatNo()));
        }
        if (team.getPlayers() != null) {
            for (com.manacommunity.sports.model.AuctionPlayer p : team.getPlayers()) {
                if (p.getUser() != null && p.getUser().getFlatNo() != null) {
                    flats.add(norm(p.getUser().getFlatNo()));
                }
            }
        }
        return flats;
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


