package com.manacommunity.sports.service.scheduler;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.dto.scheduler.PlayoffGenerateRequest;
import com.manacommunity.sports.dto.scheduler.PlayoffGenerateRequest.ParticipantInput;
import com.manacommunity.sports.dto.scheduler.PlayoffMatchDraftResponse;
import com.manacommunity.sports.dto.scheduler.PlayoffMatchDraftResponse.ParticipantRef;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateless generator for playoff ("rounds to final") brackets.
 *
 * Faithful Java port of the frontend's playoffSchedule.ts so the server
 * reproduces the exact bracket the browser used to compute locally
 * (same pairings, round names, dates and 12-hour times).
 */
@Component
public class PlayoffScheduleGenerator {

    private static final Random RANDOM = new Random();

    // ── Slot / participant helpers ────────────────────────────────────

    /** A group proceeder slot (e.g. "Winner 1 of Group 2"). */
    private record Slot(int groupIndex, int rank) {}

    private static String proceederLabel(int groupIndex, int rank) {
        return "Winner " + rank + " of Group " + (groupIndex + 1);
    }

    private static String matchWinnerLabel(String matchName) {
        return matchName + " (Winner)";
    }

    private static String slotId(int groupIndex, int rank) {
        return "G" + (groupIndex + 1) + "-W" + rank;
    }

    private static List<Slot> buildProceederSlots(int numGroups, int proceedersPerGroup) {
        List<Slot> slots = new ArrayList<>();
        for (int g = 0; g < numGroups; g++) {
            for (int r = 1; r <= proceedersPerGroup; r++) {
                slots.add(new Slot(g, r));
            }
        }
        return slots;
    }

    private static <T> List<T> shuffle(List<T> items) {
        List<T> copy = new ArrayList<>(items);
        Collections.shuffle(copy, RANDOM);
        return copy;
    }

    /**
     * Randomly chooses the index of the player to receive the BYE this round.
     * Prefers players who have not had a bye yet so the bye is shuffled fairly
     * across rounds; if every player in the round has already had one, it picks
     * randomly from the whole round.
     */
    private static int pickByeIndex(List<ParticipantRef> roundParticipants, Set<String> byeReceived) {
        List<Integer> eligible = new ArrayList<>();
        for (int i = 0; i < roundParticipants.size(); i++) {
            if (!byeReceived.contains(roundParticipants.get(i).id())) {
                eligible.add(i);
            }
        }
        if (eligible.isEmpty()) {
            for (int i = 0; i < roundParticipants.size(); i++) {
                eligible.add(i);
            }
        }
        return eligible.get(RANDOM.nextInt(eligible.size()));
    }

    private static ParticipantRef toSlotParticipant(Slot slot) {
        return new ParticipantRef(slotId(slot.groupIndex(), slot.rank()),
                proceederLabel(slot.groupIndex(), slot.rank()));
    }

    private static ParticipantRef winnerRef(String matchId, String matchName) {
        return new ParticipantRef(matchId + "-winner", matchWinnerLabel(matchName));
    }

    private static ParticipantRef loserRef(String matchId, String matchName) {
        return new ParticipantRef(matchId + "-loser", "Loser Of " + matchName);
    }

    // ── Pairing logic ─────────────────────────────────────────────────

    /** First-round pairings from group proceeder slots. */
    static List<Slot[]> buildFirstRoundPairings(List<Slot> slots, String seedingOrder, int numGroups) {
        if (slots.size() < 2) return new ArrayList<>();

        if ("SEQUENTIAL".equals(seedingOrder) && numGroups >= 2) {
            List<Slot[]> pairs = new ArrayList<>();
            for (int g = 0; g < numGroups; g += 2) {
                final int gi = g;
                List<Slot> left  = slots.stream().filter(s -> s.groupIndex() == gi).toList();
                List<Slot> right = slots.stream().filter(s -> s.groupIndex() == gi + 1).toList();
                int len = Math.min(left.size(), right.size());
                for (int i = 0; i < len; i++) {
                    pairs.add(new Slot[]{left.get(i), right.get(i)});
                }
            }
            if (!pairs.isEmpty()) return pairs;
        }

        List<Slot> ordered = new ArrayList<>(slots);
        if ("RANDOM".equals(seedingOrder)) {
            ordered = shuffle(ordered);
        }

        // TRADITIONAL (and RANDOM after shuffle): 1 vs N, 2 vs N-1 …
        List<Slot[]> pairs = new ArrayList<>();
        int lo = 0;
        int hi = ordered.size() - 1;
        while (lo < hi) {
            pairs.add(new Slot[]{ordered.get(lo), ordered.get(hi)});
            lo++;
            hi--;
        }
        return pairs;
    }

    // ── Round naming ──────────────────────────────────────────────────

    public static String getRoundName(int roundIndex, int totalRounds) {
        if (roundIndex == totalRounds - 1) return "FINAL";
        if (roundIndex == totalRounds - 2) return "SEMI_FINAL";
        if (roundIndex == totalRounds - 3) return "QUARTER_FINAL";
        return "Round " + (roundIndex + 1);
    }

    private static String roundLabel(String type, int indexInRound) {
        return switch (type) {
            case "QUARTER_FINAL" -> "Quarter Final " + (indexInRound + 1);
            case "SEMI_FINAL"    -> "Semi-Final " + (indexInRound + 1);
            case "FINAL"         -> "Final";
            case "THIRD_PLACE"   -> "Third Place";
            default              -> type + " Match " + (indexInRound + 1);
        };
    }

    // ── 12-hour time helpers (must match playoffSchedule.ts exactly) ───

    private static final Pattern TIME_12H =
            Pattern.compile("^(\\d{1,2}):(\\d{2})\\s*(AM|PM)$", Pattern.CASE_INSENSITIVE);

    /** {hours, minutes}; falls back to 08:00 when unparseable (mirrors TS). */
    static int[] parseTime12h(String time) {
        if (time == null) return new int[]{8, 0};
        Matcher m = TIME_12H.matcher(time.trim());
        if (!m.matches()) return new int[]{8, 0};
        int hours = Integer.parseInt(m.group(1));
        int minutes = Integer.parseInt(m.group(2));
        String meridiem = m.group(3).toUpperCase();
        if (meridiem.equals("PM") && hours != 12) hours += 12;
        if (meridiem.equals("AM") && hours == 12) hours = 0;
        return new int[]{hours, minutes};
    }

    static String formatTime12h(int hours, int minutes) {
        String meridiem = hours >= 12 ? "PM" : "AM";
        int h = hours % 12;
        if (h == 0) h = 12;
        return pad2(h) + ":" + pad2(minutes) + " " + meridiem;
    }

    public static String addMinutesToTime(String time, int minutesToAdd) {
        int[] hm = parseTime12h(time);
        int total = hm[0] * 60 + hm[1] + minutesToAdd;
        int newHours = (total / 60) % 24;
        int newMinutes = total % 60;
        return formatTime12h(newHours, newMinutes);
    }

    static String addDaysToDateString(String dateStr, int days) {
        String[] parts = dateStr.split("-");
        LocalDate d = parts.length == 3
                ? LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]))
                : LocalDate.parse(dateStr);
        return d.plusDays(days).toString();
    }

    private static String pad2(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    /** Sequential time/date cursor; rolls to next day when the slot passes midnight (hour < 6). */
    private static final class ScheduleCursor {
        private String slotDate;
        private String slotTime;
        private final String dayStartTime;
        private final int step;

        ScheduleCursor(String startDate, String startTime, int matchDurationMinutes, int breakMinutes) {
            this.slotDate = startDate;
            this.slotTime = startTime;
            this.dayStartTime = startTime;
            this.step = matchDurationMinutes + breakMinutes;
        }

        String[] next() {
            String[] current = {slotDate, slotTime};
            slotTime = addMinutesToTime(slotTime, step);
            if (parseTime12h(slotTime)[0] < 6) {
                slotDate = addDaysToDateString(slotDate, 1);
                slotTime = dayStartTime;
            }
            return current;
        }

        /** The day the next slot falls on, without consuming/advancing the cursor. */
        String currentDate() {
            return slotDate;
        }
    }

    // ── Bracket builders ──────────────────────────────────────────────

    /** Standard 2 groups × 2 proceeders → cross semi-finals + final. */
    private static List<PlayoffMatchDraftResponse> buildTwoGroupTwoProceederBracket(PlayoffGenerateRequest in) {
        ScheduleCursor cursor = new ScheduleCursor(
                in.startDate(), in.startTime(), in.matchDurationMinutes(), in.breakMinutes());

        Slot[] sf1Pair;
        Slot[] sf2Pair;
        if ("SEQUENTIAL".equals(in.seedingOrder())) {
            sf1Pair = new Slot[]{new Slot(0, 1), new Slot(1, 1)};
            sf2Pair = new Slot[]{new Slot(0, 2), new Slot(1, 2)};
        } else {
            sf1Pair = new Slot[]{new Slot(0, 1), new Slot(1, 2)};
            sf2Pair = new Slot[]{new Slot(0, 2), new Slot(1, 1)};
        }

        String sf1Id = "playoff-sf1";
        String sf2Id = "playoff-sf2";
        String finalId = "playoff-final";
        String[] s1 = cursor.next();
        String[] s2 = cursor.next();
        String[] sFinal = cursor.next();

        List<PlayoffMatchDraftResponse> matches = new ArrayList<>();
        matches.add(new PlayoffMatchDraftResponse(sf1Id, "Semi-Final 1", "SEMI_FINAL", 0,
                toSlotParticipant(sf1Pair[0]), toSlotParticipant(sf1Pair[1]),
                s1[0], s1[1], in.matchDurationMinutes(), in.venueId(), in.courtId(), false, null));
        matches.add(new PlayoffMatchDraftResponse(sf2Id, "Semi-Final 2", "SEMI_FINAL", 0,
                toSlotParticipant(sf2Pair[0]), toSlotParticipant(sf2Pair[1]),
                s2[0], s2[1], in.matchDurationMinutes(), in.venueId(), in.courtId(), false, null));
        matches.add(new PlayoffMatchDraftResponse(finalId, "Final", "FINAL", 1,
                winnerRef(sf1Id, "Semi-Final 1"), winnerRef(sf2Id, "Semi-Final 2"),
                sFinal[0], sFinal[1], in.matchDurationMinutes(), in.venueId(), in.courtId(), false, null));

        if (in.thirdPlaceMatch()) {
            String[] s3 = cursor.next();
            matches.add(new PlayoffMatchDraftResponse("playoff-third-place", "Third Place", "THIRD_PLACE", 2,
                    loserRef(sf1Id, "Semi-Final 1"), loserRef(sf2Id, "Semi-Final 2"),
                    s3[0], s3[1], in.matchDurationMinutes(), in.venueId(), in.courtId(), false, null));
        }
        return matches;
    }

    public List<PlayoffMatchDraftResponse> buildPlayoffBracket(PlayoffGenerateRequest in) {
        // Player-aware path: single-elimination knockout from seeded registered players
        // (BYE auto-advance, flat/tower-aware first round, parallel court allocation).
        if (in.participants() != null && in.participants().size() >= 2) {
            return buildKnockoutFromPlayers(in);
        }
        if (in.numGroups() == 2 && in.proceedersPerGroup() == 2) {
            return buildTwoGroupTwoProceederBracket(in);
        }

        List<Slot> slots = buildProceederSlots(in.numGroups(), in.proceedersPerGroup());
        if (slots.size() < 2) return new ArrayList<>();

        List<Slot[]> firstRoundPairs = buildFirstRoundPairings(slots, in.seedingOrder(), in.numGroups());
        List<ParticipantRef> current = new ArrayList<>();
        for (Slot[] pair : firstRoundPairs) {
            current.add(toSlotParticipant(pair[0]));
            current.add(toSlotParticipant(pair[1]));
        }

        // Add any unpaired leftover slots (the BYEs in round 1)
        Set<String> pairedIds = new HashSet<>();
        current.forEach(p -> pairedIds.add(p.id()));
        for (Slot slot : slots) {
            ParticipantRef p = toSlotParticipant(slot);
            if (!pairedIds.contains(p.id())) {
                current.add(p);
            }
        }

        // Simulate round sizes to find total rounds
        int simLength = current.size();
        int totalRounds = 0;
        while (simLength > 1) {
            totalRounds++;
            simLength = (int) Math.ceil(simLength / 2.0);
        }

        List<PlayoffMatchDraftResponse> matches = new ArrayList<>();
        int roundIndex = 0;
        List<ParticipantRef> roundParticipants = new ArrayList<>(current);
        ScheduleCursor cursor = new ScheduleCursor(
                in.startDate(), in.startTime(), in.matchDurationMinutes(), in.breakMinutes());

        // Track who has already received a BYE so the bye rotates fairly across
        // rounds (a player should not get a bye again until everyone else has).
        Set<String> byeReceived = new HashSet<>();

        while (roundParticipants.size() > 1) {
            String roundType = getRoundName(roundIndex, totalRounds);
            List<ParticipantRef> nextParticipants = new ArrayList<>();
            int len = roundParticipants.size();

            if (len % 2 != 0) {
                // Randomly pick the BYE among players who have NOT had one yet;
                // only if everyone in this round already has, fall back to all.
                int byeIdx = pickByeIndex(roundParticipants, byeReceived);
                ParticipantRef byeParticipant = roundParticipants.get(byeIdx);
                byeReceived.add(byeParticipant.id());

                List<ParticipantRef> activeParticipants = new ArrayList<>();
                for (int i = 0; i < len; i++) {
                    if (i != byeIdx) activeParticipants.add(roundParticipants.get(i));
                }
                int numMatches = activeParticipants.size() / 2;

                for (int mIdx = 0; mIdx < numMatches; mIdx++) {
                    ParticipantRef home = activeParticipants.get(2 * mIdx);
                    ParticipantRef away = activeParticipants.get(2 * mIdx + 1);
                    String id = "playoff-r" + roundIndex + "-m" + mIdx;
                    String name = roundLabel(roundType, mIdx);
                    String[] slot = cursor.next();
                    matches.add(new PlayoffMatchDraftResponse(id, name, roundType, roundIndex,
                            home, away, slot[0], slot[1],
                            in.matchDurationMinutes(), in.venueId(), in.courtId(), false, null));
                    nextParticipants.add(winnerRef(id, name));
                }

                // Generate the BYE match at the end of the round.
                // A bye isn't played, so it does NOT consume a time slot (the cursor is
                // not advanced) and carries no scheduled time — only the day, for display.
                int byeMatchIdx = numMatches;
                String byeMatchId = "playoff-r" + roundIndex + "-m" + byeMatchIdx;
                String byeMatchName = byeParticipant.name() + " (BYE)";
                matches.add(new PlayoffMatchDraftResponse(byeMatchId, byeMatchName, roundType, roundIndex,
                        byeParticipant, new ParticipantRef("bye", "BYE"), cursor.currentDate(), "",
                        in.matchDurationMinutes(), in.venueId(), in.courtId(), false, null));

                // Advance the BYE player directly (preserving clean name)
                nextParticipants.add(byeParticipant);
            } else {
                int numMatches = len / 2;
                for (int mIdx = 0; mIdx < numMatches; mIdx++) {
                    ParticipantRef home = roundParticipants.get(2 * mIdx);
                    ParticipantRef away = roundParticipants.get(2 * mIdx + 1);
                    String id = "playoff-r" + roundIndex + "-m" + mIdx;
                    String name = roundLabel(roundType, mIdx);
                    String[] slot = cursor.next();
                    matches.add(new PlayoffMatchDraftResponse(id, name, roundType, roundIndex,
                            home, away, slot[0], slot[1],
                            in.matchDurationMinutes(), in.venueId(), in.courtId(), false, null));
                    nextParticipants.add(winnerRef(id, name));
                }
            }

            roundParticipants = nextParticipants;
            roundIndex++;
        }

        if (in.thirdPlaceMatch()) {
            List<PlayoffMatchDraftResponse> semiMatches =
                    matches.stream().filter(m -> "SEMI_FINAL".equals(m.round())).toList();
            if (semiMatches.size() >= 2) {
                String[] slot = cursor.next();
                matches.add(new PlayoffMatchDraftResponse("playoff-third-place", "Third Place",
                        "THIRD_PLACE", roundIndex,
                        loserRef(semiMatches.get(0).id(), semiMatches.get(0).name()),
                        loserRef(semiMatches.get(1).id(), semiMatches.get(1).name()),
                        slot[0], slot[1], in.matchDurationMinutes(), in.venueId(), in.courtId(), false, null));
            }
        }

        return matches;
    }

    // ══════════════════════════════════════════════════════════════════
    // PLAYER-AWARE KNOCKOUT (single elimination from seeded registrations)
    // ══════════════════════════════════════════════════════════════════

    public List<PlayoffMatchDraftResponse> buildKnockoutFromPlayers(PlayoffGenerateRequest in) {
        List<ParticipantInput> players = in.participants();
        int n = players.size();
        if (n < 2) return new ArrayList<>();

        // Round-1 seed list (carries flat number for Rule 1/2 clash checks).
        List<ParticipantRef> seeds = new ArrayList<>();
        Map<String, Integer> ratingById = new HashMap<>();
        boolean haveRatings = false;
        for (ParticipantInput p : players) {
            seeds.add(new ParticipantRef(p.id(), p.name(), p.flatNumber()));
            if (p.rating() != null) {
                ratingById.put(p.id(), p.rating());
                haveRatings = true;
            }
        }

        // Rule 6 — balance skill levels: order round-1 players by rating so that
        // similar ratings meet (5 vs 4, not 5 vs 1); neighbours are then paired.
        if (haveRatings) {
            seeds.sort(Comparator.comparingInt(
                    (ParticipantRef r) -> ratingById.getOrDefault(r.id(), Integer.MIN_VALUE)).reversed());
        }

        // Rule 3 — total rounds = ceil(log2(n)); exactly ONE bye is injected on
        // each round whose participant count is odd (cascading down the bracket).
        int totalRounds = 0;
        for (int s = 1; s < n; s <<= 1) totalRounds++;

        List<Long> courtIds = new ArrayList<>();
        if (in.courtIds() != null) courtIds.addAll(in.courtIds());
        if (courtIds.isEmpty() && in.courtId() != null) courtIds.add(in.courtId());

        WaveScheduler sched = new WaveScheduler(
                in.startDate(), in.startTime(), in.matchDurationMinutes(), in.breakMinutes(), courtIds);

        List<PlayoffMatchDraftResponse> matches = new ArrayList<>();
        Set<String> byeReceived = new HashSet<>();   // Rule 4 — avoid repeat byes
        List<ParticipantRef> roundParticipants = new ArrayList<>(seeds);
        int roundIndex = 0;

        while (roundParticipants.size() > 1) {
            String roundType = getRoundName(roundIndex, totalRounds);
            List<ParticipantRef> active = new ArrayList<>(roundParticipants);
            List<ParticipantRef> next = new ArrayList<>();

            // Rule 3 + 4: when the round has an odd count, pull exactly ONE bye,
            // chosen randomly and preferring players who have not had a bye yet.
            ParticipantRef byePlayer = null;
            if (active.size() % 2 != 0) {
                int byeIdx = pickByeIndex(active, byeReceived);
                byePlayer = active.remove(byeIdx);
                byeReceived.add(byePlayer.id());
            }

            // Pair the remaining (even) participants. Round 1 is skill-ordered, so
            // neighbour pairing keeps ratings close; later rounds pair placeholders.
            List<ParticipantRef[]> pairs = neighborPairs(active);
            if (roundIndex == 0) applyConstraints(pairs);   // Rules 1 & 2 (first round only)

            int mIdx = 0;
            for (ParticipantRef[] pr : pairs) {
                String id = "ko-r" + roundIndex + "-m" + mIdx;
                String name = roundLabel(roundType, mIdx);
                TimedSlot s = sched.nextSlot();
                matches.add(new PlayoffMatchDraftResponse(id, name, roundType, roundIndex,
                        pr[0], pr[1], s.date(), s.time(), in.matchDurationMinutes(),
                        in.venueId(), s.courtId(), false, null));
                next.add(winnerRef(id, name));
                mIdx++;
            }

            // Rule 5: the BYE match is not played — no court, no time, AUTO_ADVANCED;
            // the bye player advances directly to the next round.
            if (byePlayer != null) {
                String byeId = "ko-r" + roundIndex + "-bye";
                matches.add(new PlayoffMatchDraftResponse(byeId, byePlayer.name() + " (BYE)",
                        roundType, roundIndex, byePlayer, new ParticipantRef("bye", "BYE"),
                        sched.currentDate(), "", in.matchDurationMinutes(),
                        in.venueId(), null, false, "AUTO_ADVANCED"));
                next.add(byePlayer);
            }

            sched.flushWave();   // Rules 7 & 8 — fresh time-slot wave per round boundary
            roundParticipants = next;
            roundIndex++;
        }

        // Optional third-place match (losers of the two semi-finals).
        if (in.thirdPlaceMatch()) {
            List<PlayoffMatchDraftResponse> semis =
                    matches.stream().filter(m -> "SEMI_FINAL".equals(m.round())).toList();
            if (semis.size() >= 2) {
                TimedSlot s = sched.nextSlot();
                matches.add(new PlayoffMatchDraftResponse("ko-third-place", "Third Place",
                        "THIRD_PLACE", roundIndex,
                        loserRef(semis.get(0).id(), semis.get(0).name()),
                        loserRef(semis.get(1).id(), semis.get(1).name()),
                        s.date(), s.time(), in.matchDurationMinutes(),
                        in.venueId(), s.courtId(), false, null));
            }
        }

        return matches;
    }

    /** Pair an even-sized list as neighbours: (0,1),(2,3),… (keeps Rule-6 skill order). */
    private static List<ParticipantRef[]> neighborPairs(List<ParticipantRef> players) {
        List<ParticipantRef[]> pairs = new ArrayList<>();
        for (int i = 0; i + 1 < players.size(); i += 2) {
            pairs.add(new ParticipantRef[]{players.get(i), players.get(i + 1)});
        }
        return pairs;
    }

    /**
     * Best-effort first-round constraint pass: for any pair where the two players share a flat
     * (Rule 1) or a tower (Rule 2), try swapping the away players with another pair so both pairs
     * become conflict-free. Leaves the pair unchanged if no valid swap exists.
     */
    private static void applyConstraints(List<ParticipantRef[]> pairs) {
        for (int i = 0; i < pairs.size(); i++) {
            if (!conflict(pairs.get(i)[0], pairs.get(i)[1])) continue;
            for (int j = 0; j < pairs.size(); j++) {
                if (j == i) continue;
                ParticipantRef awayI = pairs.get(i)[1];
                ParticipantRef awayJ = pairs.get(j)[1];
                if (!conflict(pairs.get(i)[0], awayJ) && !conflict(pairs.get(j)[0], awayI)) {
                    pairs.get(i)[1] = awayJ;
                    pairs.get(j)[1] = awayI;
                    break;
                }
            }
        }
    }

    /** Two players conflict when they share a flat (Rule 1) or a tower (Rule 2). */
    private static boolean conflict(ParticipantRef a, ParticipantRef b) {
        String fa = norm(a.flatNumber());
        String fb = norm(b.flatNumber());
        if (!fa.isEmpty() && fa.equals(fb)) return true;          // same flat
        String ta = tower(a.flatNumber());
        String tb = tower(b.flatNumber());
        return !ta.isEmpty() && ta.equals(tb);                    // same tower
    }

    /** Leading letters of a flat number (e.g. "A302" → "A", "A-305" → "A", "302" → ""). */
    private static String tower(String flat) {
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

    private record TimedSlot(String date, String time, Long courtId) {}

    /**
     * Parallel court allocator (Rule 4): assigns up to {@code courtIds.size()} matches at the same
     * time across the available courts, then advances one wave (duration + break).
     */
    private static final class WaveScheduler {
        private String date;
        private String time;
        private final String dayStart;
        private final int step;
        private final List<Long> courtIds;
        private int courtPtr = 0;

        WaveScheduler(String startDate, String startTime, int durationMinutes, int breakMinutes,
                      List<Long> courtIds) {
            this.date = startDate;
            this.time = startTime;
            this.dayStart = startTime;
            this.step = durationMinutes + breakMinutes;
            this.courtIds = courtIds;
        }

        TimedSlot nextSlot() {
            Long court = courtIds.isEmpty() ? null : courtIds.get(courtPtr % courtIds.size());
            TimedSlot slot = new TimedSlot(date, time, court);
            courtPtr++;
            if (courtPtr >= Math.max(1, courtIds.size())) {
                advance();
                courtPtr = 0;
            }
            return slot;
        }

        /** Start a fresh wave at a round boundary so rounds don't share a time slot. */
        void flushWave() {
            if (courtPtr > 0) {
                advance();
                courtPtr = 0;
            }
        }

        String currentDate() {
            return date;
        }

        private void advance() {
            time = addMinutesToTime(time, step);
            if (parseTime12h(time)[0] < 6) {
                date = addDaysToDateString(date, 1);
                time = dayStart;
            }
        }
    }
}

