package com.manacommunity.sports.slice.controller;

import com.manacommunity.sports.controller.scheduler.TournamentSchedulerController;
import com.manacommunity.sports.dto.scheduler.BulkMatchSaveRequest;

import com.manacommunity.sports.repository.AuctionConfigRepository;
import com.manacommunity.sports.repository.SportsEventRepository;
import com.manacommunity.sports.repository.scheduler.TournamentConfigRepository;
import com.manacommunity.sports.repository.scheduler.TournamentMatchRepository;
import com.manacommunity.sports.dto.scheduler.PlayoffGenerateRequest;
import com.manacommunity.sports.dto.scheduler.PlayoffMatchDraftResponse;
import com.manacommunity.sports.user.service.LoggedInUserService;
import com.manacommunity.sports.service.scheduler.PlayoffScheduleGenerator;
import com.manacommunity.sports.service.scheduler.TournamentSchedulerService;
import com.manacommunity.sports.support.BaseWebMvcTest;
import com.manacommunity.sports.support.TestDataBuilder;
import com.manacommunity.sports.support.WithMockUserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TournamentSchedulerController.class)
@DisplayName("TournamentSchedulerController")
class TournamentSchedulerControllerTest extends BaseWebMvcTest {

    @MockitoBean TournamentSchedulerService  schedulerService;
    @MockitoBean TournamentConfigRepository  configRepo;
    @MockitoBean TournamentMatchRepository   matchRepo;
    @MockitoBean SportsEventRepository       eventRepo;
    @MockitoBean LoggedInUserService         loggedInUserService;
    @MockitoBean AuctionConfigRepository     auctionConfigRepo;
    @MockitoBean PlayoffScheduleGenerator    playoffGenerator;

    // ── GET /api/tournament/{configId}/matches ────────────────────────

    @Nested
    @DisplayName("GET /api/tournament/{configId}/matches")
    class GetMatches {

        @Test
        @WithMockUserPrincipal(role = "ADMIN")
        @DisplayName("returns 200 with match list")
        void returns200WithMatches() throws Exception {
            when(matchRepo.findByConfigId(84L)).thenReturn(List.of());
            // findByConfigId returns empty → no toMatchResponse calls needed

            mockMvc.perform(get("/api/tournament/84/matches"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("unauthenticated returns 4xx")
        void unauthenticated_4xx() throws Exception {
            mockMvc.perform(get("/api/tournament/84/matches"))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ── POST /api/tournament/{configId}/matches/bulk ──────────────────

    @Nested
    @DisplayName("POST /api/tournament/{configId}/matches/bulk")
    class BulkSave {

        private BulkMatchSaveRequest buildRequest(int count) {
            List<BulkMatchSaveRequest.MatchData> matches = new java.util.ArrayList<>();
            for (int i = 1; i <= count; i++) {
                matches.add(new BulkMatchSaveRequest.MatchData(
                        10L, 84L, "Group A", "GROUP", i,
                        "Team A", "1", "Team B", "2",
                        LocalDate.now().toString(), "10:00", 90, 1L, 1L, "SCHEDULED"));
            }
            return new BulkMatchSaveRequest(matches);
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN")
        @DisplayName("ADMIN can bulk-save matches and gets count back")
        void admin_bulkSave_returns200() throws Exception {
            when(schedulerService.saveMatchesBulk(eq(84L), any())).thenReturn(4);

            mockMvc.perform(post("/api/tournament/84/matches/bulk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(buildRequest(4))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.saved").value(4))
                    .andExpect(jsonPath("$.configId").value(84));
        }

        @Test
        @WithMockUserPrincipal(role = "MEMBER")
        @DisplayName("MEMBER role is rejected with 403")
        void member_rejected_403() throws Exception {
            mockMvc.perform(post("/api/tournament/84/matches/bulk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(buildRequest(1))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unauthenticated request returns 4xx")
        void unauthenticated_4xx() throws Exception {
            mockMvc.perform(post("/api/tournament/84/matches/bulk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(buildRequest(1))))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ── POST /api/tournament/playoff/generate ─────────────────────────

    @Nested
    @DisplayName("POST /api/tournament/playoff/generate")
    class GeneratePlayoff {

        private PlayoffGenerateRequest input() {
            return new PlayoffGenerateRequest(
                    2, 2, "TRADITIONAL", true,
                    "2026-06-20", "08:00 AM", 30, 10, 1L, 1L, null, null);
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN")
        @DisplayName("ADMIN gets 200 with the generated bracket")
        void admin_returns200() throws Exception {
            when(playoffGenerator.buildPlayoffBracket(any())).thenReturn(List.of(
                    new PlayoffMatchDraftResponse("playoff-final", "Final", "FINAL", 1,
                            new PlayoffMatchDraftResponse.ParticipantRef("a", "A"),
                            new PlayoffMatchDraftResponse.ParticipantRef("b", "B"),
                            "2026-06-20", "09:20 AM", 30, 1L, 1L, false, null)));

            mockMvc.perform(post("/api/tournament/playoff/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(input())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].round").value("FINAL"));
        }

        @Test
        @WithMockUserPrincipal(role = "MEMBER")
        @DisplayName("MEMBER role is rejected with 403")
        void member_rejected_403() throws Exception {
            mockMvc.perform(post("/api/tournament/playoff/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(input())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unauthenticated request returns 4xx")
        void unauthenticated_4xx() throws Exception {
            mockMvc.perform(post("/api/tournament/playoff/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(input())))
                    .andExpect(status().is4xxClientError());
        }
    }
}
