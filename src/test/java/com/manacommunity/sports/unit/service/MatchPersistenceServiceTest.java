package com.manacommunity.sports.unit.service;

import com.manacommunity.sports.dto.scheduler.BulkMatchSaveRequest;
import com.manacommunity.sports.model.scheduler.*;
import com.manacommunity.sports.repository.AuctionTeamRepository;
import com.manacommunity.sports.user.repository.AppUserRepository;
import com.manacommunity.sports.repository.CommunityRepository;
import com.manacommunity.sports.repository.CourtRepository;
import com.manacommunity.sports.repository.SportMetaRepository;
import com.manacommunity.sports.repository.SportsEventRepository;
import com.manacommunity.sports.repository.VenueRepository;
import com.manacommunity.sports.repository.scheduler.*;
import com.manacommunity.sports.service.scheduler.MatchPersistenceService;
import com.manacommunity.sports.service.scheduler.TimeSlotAllocator;
import com.manacommunity.sports.support.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchPersistenceService - saveMatchesBulk")
class MatchPersistenceServiceTest {

    @Mock TournamentConfigRepository  configRepo;
    @Mock TournamentGroupRepository   groupRepo;
    @Mock TournamentMatchRepository   matchRepo;
    @Mock GroupTeamStandingRepository standingRepo;
    @Mock AuctionTeamRepository       teamRepo;
    @Mock AppUserRepository           userRepo;
    @Mock SportMetaRepository         sportMetaRepo;
    @Mock CommunityRepository         communityRepo;
    @Mock SportsEventRepository       eventRepo;
    @Mock VenueRepository             venueRepo;
    @Mock CourtRepository             courtRepo;
    @Mock TimeSlotAllocator           timeSlots;

    @InjectMocks MatchPersistenceService service;

    private TournamentConfig config() {
        return TestDataBuilder.tournamentConfig(84L);
    }

    private BulkMatchSaveRequest.MatchData matchData(String stage, String homeName, String awayName) {
        return new BulkMatchSaveRequest.MatchData(
                10L, 84L, "Group A", stage,
                1, homeName, "1",
                awayName, "2",
                LocalDate.now().plusDays(7).toString(), "10:00",
                90, null, null, "SCHEDULED"
        );
    }

    @Nested
    @DisplayName("saveMatchesBulk")
    class SaveBulk {

        @Test
        @DisplayName("deletes existing matches and saves new ones")
        void deletesAndSaves() {
            TournamentConfig config = config();
            when(configRepo.findById(84L)).thenReturn(Optional.of(config));
            when(teamRepo.findById(1L)).thenReturn(Optional.empty());
            when(teamRepo.findById(2L)).thenReturn(Optional.empty());

            List<BulkMatchSaveRequest.MatchData> matches = List.of(
                    matchData("GROUP", "Team A", "Team B"),
                    matchData("GROUP", "Team C", "Team D")
            );

            int saved = service.saveMatchesBulk(84L, matches);

            assertThat(saved).isEqualTo(2);
            verify(matchRepo).deleteByConfigId(84L);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<TournamentMatch>> captor = ArgumentCaptor.forClass(List.class);
            verify(matchRepo).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("GROUP stage maps to GROUP_STAGE round")
        void groupStageRound() {
            when(configRepo.findById(84L)).thenReturn(Optional.of(config()));
            when(teamRepo.findById(anyLong())).thenReturn(Optional.empty());

            service.saveMatchesBulk(84L, List.of(matchData("GROUP", "A", "B")));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<TournamentMatch>> captor = ArgumentCaptor.forClass(List.class);
            verify(matchRepo).saveAll(captor.capture());
            assertThat(captor.getValue().get(0).getRound()).isEqualTo(MatchRound.GROUP_STAGE);
        }

        @Test
        @DisplayName("PLAYOFF stage maps to SEMI_FINAL round")
        void playoffRound() {
            when(configRepo.findById(84L)).thenReturn(Optional.of(config()));
            when(teamRepo.findById(anyLong())).thenReturn(Optional.empty());

            service.saveMatchesBulk(84L, List.of(matchData("PLAYOFF", "A", "B")));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<TournamentMatch>> captor = ArgumentCaptor.forClass(List.class);
            verify(matchRepo).saveAll(captor.capture());
            assertThat(captor.getValue().get(0).getRound()).isEqualTo(MatchRound.SEMI_FINAL);
        }

        @Test
        @DisplayName("KNOCKOUT stage maps to FINAL round")
        void knockoutRound() {
            when(configRepo.findById(84L)).thenReturn(Optional.of(config()));
            when(teamRepo.findById(anyLong())).thenReturn(Optional.empty());

            service.saveMatchesBulk(84L, List.of(matchData("KNOCKOUT", "A", "B")));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<TournamentMatch>> captor = ArgumentCaptor.forClass(List.class);
            verify(matchRepo).saveAll(captor.capture());
            assertThat(captor.getValue().get(0).getRound()).isEqualTo(MatchRound.FINAL);
        }

        @Test
        @DisplayName("TBD team ID does not trigger a DB lookup")
        void tbdTeamNotLookedUp() {
            when(configRepo.findById(84L)).thenReturn(Optional.of(config()));

            BulkMatchSaveRequest.MatchData m = new BulkMatchSaveRequest.MatchData(
                    10L, 84L, "Playoffs", "KNOCKOUT", 1,
                    "Winner A", "TBD", "Winner B", "TBD",
                    LocalDate.now().toString(), "12:00", 60, null, null, "SCHEDULED");

            service.saveMatchesBulk(84L, List.of(m));

            verify(teamRepo, never()).findById(anyLong());
        }

        @Test
        @DisplayName("throws IllegalArgumentException for unknown configId")
        void unknownConfig() {
            when(configRepo.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.saveMatchesBulk(999L, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("match notes contain team names as JSON")
        void matchNotesContainTeamNames() {
            when(configRepo.findById(84L)).thenReturn(Optional.of(config()));
            when(teamRepo.findById(anyLong())).thenReturn(Optional.empty());

            service.saveMatchesBulk(84L, List.of(matchData("GROUP", "Alpha FC", "Beta SC")));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<TournamentMatch>> captor = ArgumentCaptor.forClass(List.class);
            verify(matchRepo).saveAll(captor.capture());
            String notes = captor.getValue().get(0).getMatchNotes();
            assertThat(notes).contains("Alpha FC").contains("Beta SC");
        }
    }
}
