package com.manacommunity.sports.slice.controller;

import com.manacommunity.sports.controller.SportsController;
import com.manacommunity.sports.model.SportsMeta;
import com.manacommunity.sports.repository.PlayerCategoryRepository;
import com.manacommunity.sports.repository.SportMetaRepository;
import com.manacommunity.sports.user.service.LoggedInUserService;
import com.manacommunity.sports.service.PermissionCheckService;
import com.manacommunity.sports.service.SportsEventCsvImportService;
import com.manacommunity.sports.service.SportsEventService;
import com.manacommunity.sports.service.TournamentService;
import com.manacommunity.sports.support.BaseWebMvcTest;
import com.manacommunity.sports.support.WithMockUserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SportsController.class)
@DisplayName("SportsController")
class SportsControllerTest extends BaseWebMvcTest {

    @MockitoBean SportsEventService         eventService;
    @MockitoBean SportMetaRepository        sportMetaRepo;
    @MockitoBean PlayerCategoryRepository   categoryRepo;
    @MockitoBean LoggedInUserService        loggedInUserService;
    @MockitoBean TournamentService          tournamentService;
    @MockitoBean PermissionCheckService     permissionCheckService;
    @MockitoBean SportsEventCsvImportService csvImportService;

    // ── GET /api/sports/meta ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/sports/meta")
    class GetMeta {

        @Test
        @WithMockUserPrincipal(role = "ADMIN")
        @DisplayName("authorized user gets 200 with sport list")
        void authorized_returns200() throws Exception {
            SportsMeta badminton = new SportsMeta();
            badminton.setId(1L);
            badminton.setName("Badminton");

            doNothing().when(permissionCheckService).requireAnyPermission(any(), any());
            when(sportMetaRepo.findByActiveTrue()).thenReturn(List.of(badminton));

            mockMvc.perform(get("/api/sports/meta"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Badminton"));
        }

        @Test
        @WithMockUserPrincipal(role = "MEMBER")
        @DisplayName("missing permission returns 403")
        void forbidden_returns403() throws Exception {
            doThrow(new AccessDeniedException("Insufficient permissions"))
                    .when(permissionCheckService).requireAnyPermission(any(), any());

            mockMvc.perform(get("/api/sports/meta"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unauthenticated request returns 401 or 403")
        void unauthenticated_returns4xx() throws Exception {
            mockMvc.perform(get("/api/sports/meta"))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ── GET /api/sports/events ────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/sports/events")
    class GetEvents {

        @Test
        @WithMockUserPrincipal(role = "ADMIN")
        @DisplayName("returns 200 with empty list when no events exist")
        void emptyEvents_returns200() throws Exception {
            doNothing().when(permissionCheckService).requireAnyPermission(any(), any());
            when(eventService.getAllEvents(any())).thenReturn(org.springframework.data.domain.Page.empty());

            mockMvc.perform(get("/api/sports/events"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }
}
