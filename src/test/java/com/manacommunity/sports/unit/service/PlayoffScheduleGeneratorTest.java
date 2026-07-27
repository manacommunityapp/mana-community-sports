package com.manacommunity.sports.unit.service;

import com.manacommunity.sports.dto.scheduler.PlayoffGenerateRequest;
import com.manacommunity.sports.dto.scheduler.PlayoffMatchDraftResponse;
import com.manacommunity.sports.service.scheduler.PlayoffScheduleGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlayoffScheduleGenerator - rounds-to-final bracket")
class PlayoffScheduleGeneratorTest {

    private final PlayoffScheduleGenerator generator = new PlayoffScheduleGenerator();

    private PlayoffGenerateRequest request(int numGroups, int proceeders, String seeding,
                                           boolean thirdPlace) {
        return new PlayoffGenerateRequest(
                numGroups, proceeders, seeding, thirdPlace,
                "2026-06-20", "08:00 AM", 30, 10, 1L, 1L, null, null);
    }

    // ── Player-aware knockout helpers ─────────────────────────────────

    private static PlayoffGenerateRequest.ParticipantInput player(String id, String name, String flat) {
        return new PlayoffGenerateRequest.ParticipantInput(id, name, flat);
    }

    private PlayoffGenerateRequest knockoutRequest(
            List<PlayoffGenerateRequest.ParticipantInput> players, List<Long> courtIds) {
        return new PlayoffGenerateRequest(
                1, players.size(), "TRADITIONAL", false,
                "2026-06-20", "09:00 AM", 30, 10, 1L, null, players, courtIds);
    }

    @Nested
    @DisplayName("2 groups x 2 proceeders")
    class TwoGroupTwoProceeder {

        @Test
        @DisplayName("produces cross semi-finals + final")
        void crossSemisAndFinal() {
            List<PlayoffMatchDraftResponse> matches =
                    generator.buildPlayoffBracket(request(2, 2, "TRADITIONAL", false));

            assertThat(matches).hasSize(3);
            assertThat(matches).extracting(PlayoffMatchDraftResponse::round)
                    .containsExactly("SEMI_FINAL", "SEMI_FINAL", "FINAL");

            // TRADITIONAL cross seeding: SF1 = G1-W1 vs G2-W2, SF2 = G1-W2 vs G2-W1
            assertThat(matches.get(0).home().id()).isEqualTo("G1-W1");
            assertThat(matches.get(0).away().id()).isEqualTo("G2-W2");
            assertThat(matches.get(1).home().id()).isEqualTo("G1-W2");
            assertThat(matches.get(1).away().id()).isEqualTo("G2-W1");

            // Final is fed by the two semi-final winners
            assertThat(matches.get(2).home().id()).isEqualTo("playoff-sf1-winner");
            assertThat(matches.get(2).away().id()).isEqualTo("playoff-sf2-winner");
        }

        @Test
        @DisplayName("SEQUENTIAL seeding pairs same ranks across groups")
        void sequentialSeeding() {
            List<PlayoffMatchDraftResponse> matches =
                    generator.buildPlayoffBracket(request(2, 2, "SEQUENTIAL", false));

            assertThat(matches.get(0).home().id()).isEqualTo("G1-W1");
            assertThat(matches.get(0).away().id()).isEqualTo("G2-W1");
            assertThat(matches.get(1).home().id()).isEqualTo("G1-W2");
            assertThat(matches.get(1).away().id()).isEqualTo("G2-W2");
        }

        @Test
        @DisplayName("third-place flag adds a THIRD_PLACE match from semi losers")
        void thirdPlaceAdded() {
            List<PlayoffMatchDraftResponse> matches =
                    generator.buildPlayoffBracket(request(2, 2, "TRADITIONAL", true));

            assertThat(matches).hasSize(4);
            PlayoffMatchDraftResponse third = matches.get(3);
            assertThat(third.round()).isEqualTo("THIRD_PLACE");
            assertThat(third.home().id()).isEqualTo("playoff-sf1-loser");
            assertThat(third.away().id()).isEqualTo("playoff-sf2-loser");
        }

        @Test
        @DisplayName("schedules matches sequentially with break between slots")
        void sequentialSlots() {
            List<PlayoffMatchDraftResponse> matches =
                    generator.buildPlayoffBracket(request(2, 2, "TRADITIONAL", false));

            // 30m duration + 10m break = 40m steps from 08:00 AM
            assertThat(matches.get(0).time()).isEqualTo("08:00 AM");
            assertThat(matches.get(1).time()).isEqualTo("08:40 AM");
            assertThat(matches.get(2).time()).isEqualTo("09:20 AM");
            assertThat(matches).allSatisfy(m -> {
                assertThat(m.date()).isEqualTo("2026-06-20");
                assertThat(m.venueId()).isEqualTo(1L);
                assertThat(m.courtId()).isEqualTo(1L);
                assertThat(m.duration()).isEqualTo(30);
            });
        }
    }

    @Nested
    @DisplayName("general bracket")
    class GeneralBracket {

        @Test
        @DisplayName("4 groups x 1 proceeder => semis + final, names FINAL/SEMI_FINAL")
        void fourTeams() {
            List<PlayoffMatchDraftResponse> matches =
                    generator.buildPlayoffBracket(request(4, 1, "TRADITIONAL", false));

            assertThat(matches).extracting(PlayoffMatchDraftResponse::round)
                    .containsExactly("SEMI_FINAL", "SEMI_FINAL", "FINAL");
            // Final winner refs point at the semi matches generated this round
            assertThat(matches.get(2).home().id()).contains("winner");
            assertThat(matches.get(2).away().id()).contains("winner");
        }

        @Test
        @DisplayName("odd participant count produces a BYE match and advances the player")
        void oddCountByes() {
            // 3 groups x 1 proceeder = 3 participants -> one BYE in round 1
            List<PlayoffMatchDraftResponse> matches =
                    generator.buildPlayoffBracket(request(3, 1, "TRADITIONAL", false));

            assertThat(matches).anySatisfy(m -> assertThat(m.name()).contains("(BYE)"));
            // A bracket of 3 collapses to a final, so a FINAL must exist
            assertThat(matches).anySatisfy(m -> assertThat(m.round()).isEqualTo("FINAL"));

            // A bye isn't played: it carries no scheduled time (only a day for display).
            PlayoffMatchDraftResponse bye = matches.stream()
                    .filter(m -> m.name().contains("(BYE)")).findFirst().orElseThrow();
            assertThat(bye.time()).isEmpty();

            // The bye does not consume a slot, so real matches keep distinct, advancing times.
            long distinctRealTimes = matches.stream()
                    .filter(m -> !m.name().contains("(BYE)"))
                    .map(PlayoffMatchDraftResponse::time)
                    .distinct().count();
            long realMatches = matches.stream().filter(m -> !m.name().contains("(BYE)")).count();
            assertThat(distinctRealTimes).isEqualTo(realMatches);
        }
    }

    @Nested
    @DisplayName("round naming")
    class RoundNaming {
        @Test
        @DisplayName("getRoundName maps last three rounds to FINAL/SEMI/QUARTER")
        void roundNames() {
            assertThat(PlayoffScheduleGenerator.getRoundName(2, 3)).isEqualTo("FINAL");
            assertThat(PlayoffScheduleGenerator.getRoundName(1, 3)).isEqualTo("SEMI_FINAL");
            assertThat(PlayoffScheduleGenerator.getRoundName(0, 3)).isEqualTo("QUARTER_FINAL");
            assertThat(PlayoffScheduleGenerator.getRoundName(0, 5)).isEqualTo("Round 1");
        }
    }

    @Nested
    @DisplayName("12-hour time helpers")
    class TimeHelpers {
        @Test
        @DisplayName("addMinutesToTime rolls across the hour and noon correctly")
        void addMinutes() {
            assertThat(PlayoffScheduleGenerator.addMinutesToTime("08:00 AM", 40)).isEqualTo("08:40 AM");
            assertThat(PlayoffScheduleGenerator.addMinutesToTime("11:50 AM", 20)).isEqualTo("12:10 PM");
            assertThat(PlayoffScheduleGenerator.addMinutesToTime("11:30 PM", 40)).isEqualTo("12:10 AM");
        }
    }

    @Nested
    @DisplayName("player-aware knockout (BYEs / constraints / courts)")
    class KnockoutFromPlayers {

        /** n players, each with a distinct tower (A,B,C…) so there are no flat/tower clashes. */
        private List<PlayoffGenerateRequest.ParticipantInput> players(int n) {
            List<PlayoffGenerateRequest.ParticipantInput> list = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                char tower = (char) ('A' + i);
                list.add(player("p" + i, "Player " + (i + 1), tower + "101"));
            }
            return list;
        }

        private String tower(String flat) {
            return flat == null ? "" : flat.replaceAll("\\d.*$", "").toUpperCase();
        }

        @Test
        @DisplayName("13 players → 3 BYEs to top seeds, all AUTO_ADVANCED, players auto-advance")
        void thirteenPlayersThreeByes() {
            List<PlayoffMatchDraftResponse> matches =
                    generator.buildKnockoutFromPlayers(knockoutRequest(players(13), List.of(1L, 2L, 3L, 4L)));

            List<PlayoffMatchDraftResponse> byes = matches.stream()
                    .filter(m -> "AUTO_ADVANCED".equals(m.status())).toList();

            assertThat(byes).hasSize(3); // nextPow2(13)=16, 16-13=3
            assertThat(byes).allSatisfy(b -> {
                assertThat(b.name()).contains("(BYE)");
                assertThat(b.away().id()).isEqualTo("bye");
                assertThat(b.courtId()).isNull();   // a bye isn't played on a court
                assertThat(b.time()).isEmpty();      // …and carries no scheduled time
            });
            // BYEs go to the top 3 seeds (the first 3 in registration order)
            assertThat(byes).extracting(b -> b.home().name())
                    .containsExactlyInAnyOrder("Player 1", "Player 2", "Player 3");
        }

        @Test
        @DisplayName("power-of-two player count → 0 BYEs")
        void powerOfTwoNoByes() {
            List<PlayoffMatchDraftResponse> matches =
                    generator.buildKnockoutFromPlayers(knockoutRequest(players(8), List.of(1L, 2L)));
            assertThat(matches.stream().filter(m -> "AUTO_ADVANCED".equals(m.status())).count()).isZero();
        }

        @Test
        @DisplayName("Rule 1 + 2: first round avoids same flat and same tower")
        void avoidsSameFlatAndTower() {
            // Default seed pairing of [P1,P2,P3,P4] is (P1,P4),(P2,P3); P1 & P4 share tower A,
            // so the constraint pass must swap to fix it.
            List<PlayoffGenerateRequest.ParticipantInput> ps = List.of(
                    player("p1", "P1", "A101"),
                    player("p2", "P2", "B101"),
                    player("p3", "P3", "C101"),
                    player("p4", "P4", "A202"));

            List<PlayoffMatchDraftResponse> round1 =
                    generator.buildKnockoutFromPlayers(knockoutRequest(ps, List.of(1L))).stream()
                            .filter(m -> m.roundIndex() == 0 && !"bye".equals(m.away().id())).toList();

            assertThat(round1).isNotEmpty();
            assertThat(round1).allSatisfy(m -> {
                assertThat(m.home().flatNumber()).isNotEqualToIgnoringCase(m.away().flatNumber()); // Rule 1
                assertThat(tower(m.home().flatNumber())).isNotEqualTo(tower(m.away().flatNumber())); // Rule 2
            });
        }

        @Test
        @DisplayName("Rule 4: parallel court allocation in waves")
        void parallelCourtWaves() {
            // 16 players (no BYEs) → 8 first-round matches across 4 courts.
            List<PlayoffMatchDraftResponse> round1 =
                    generator.buildKnockoutFromPlayers(knockoutRequest(players(16), List.of(10L, 11L, 12L, 13L)))
                            .stream().filter(m -> m.roundIndex() == 0).toList();

            assertThat(round1).hasSize(8);
            // Wave 1: first 4 matches at 09:00 AM on courts 10,11,12,13
            assertThat(round1.subList(0, 4)).allSatisfy(m -> assertThat(m.time()).isEqualTo("09:00 AM"));
            assertThat(round1.subList(0, 4)).extracting(PlayoffMatchDraftResponse::courtId)
                    .containsExactly(10L, 11L, 12L, 13L);
            // Wave 2: next 4 matches at 09:40 AM (09:00 + 30 duration + 10 break)
            assertThat(round1.subList(4, 8)).allSatisfy(m -> assertThat(m.time()).isEqualTo("09:40 AM"));
        }
    }
}
