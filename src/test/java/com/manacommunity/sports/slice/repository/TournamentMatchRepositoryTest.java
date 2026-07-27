package com.manacommunity.sports.slice.repository;

import com.manacommunity.sports.model.scheduler.*;
import com.manacommunity.sports.repository.scheduler.TournamentConfigRepository;
import com.manacommunity.sports.repository.scheduler.TournamentMatchRepository;
import com.manacommunity.sports.support.BaseRepositoryTest;
import com.manacommunity.sports.support.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TournamentMatchRepository")
class TournamentMatchRepositoryTest extends BaseRepositoryTest {

    @Autowired TournamentMatchRepository  matchRepo;
    @Autowired TournamentConfigRepository configRepo;
    @Autowired TestEntityManager em;

    private TournamentConfig config;

    @BeforeEach
    void setUp() {
        TournamentConfig c = TestDataBuilder.tournamentConfig(null);
        config = em.persistAndFlush(c);
        em.clear();
    }

    private TournamentMatch saveMatch(int number) {
        TournamentMatch m = TestDataBuilder.match(config, number);
        return em.persistAndFlush(m);
    }

    @Nested
    @DisplayName("findByConfigId")
    class FindByConfig {

        @Test
        @DisplayName("returns all matches for a config")
        void returnsMatchesForConfig() {
            saveMatch(1);
            saveMatch(2);
            em.clear();

            List<TournamentMatch> result = matchRepo.findByConfigId(config.getId());
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when no matches exist")
        void emptyWhenNoMatches() {
            assertThat(matchRepo.findByConfigId(config.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteByConfigId")
    class DeleteByConfig {

        @Test
        @DisplayName("removes all matches for the config")
        void deletesAll() {
            saveMatch(1);
            saveMatch(2);
            em.clear();

            matchRepo.deleteByConfigId(config.getId());
            em.flush();
            em.clear();

            assertThat(matchRepo.findByConfigId(config.getId())).isEmpty();
        }

        @Test
        @DisplayName("does not affect matches of a different config")
        void doesNotAffectOtherConfigs() {
            TournamentConfig otherConfig = em.persistAndFlush(TestDataBuilder.tournamentConfig(null));
            TournamentMatch otherMatch = TestDataBuilder.match(otherConfig, 1);
            em.persistAndFlush(otherMatch);

            saveMatch(1);
            em.clear();

            matchRepo.deleteByConfigId(config.getId());
            em.flush();
            em.clear();

            assertThat(matchRepo.findByConfigId(otherConfig.getId())).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByConfigIdOrderByScheduledAt")
    class FindOrdered {

        @Test
        @DisplayName("returns matches in scheduledAt order")
        void orderedByScheduledAt() {
            saveMatch(2);
            saveMatch(1);
            em.clear();

            List<TournamentMatch> result = matchRepo.findByConfigIdOrderByScheduledAt(config.getId());
            assertThat(result).hasSize(2);
        }
    }

    @Test
    @DisplayName("saved match has SCHEDULED status")
    void matchStatusIsScheduled() {
        TournamentMatch saved = saveMatch(1);
        assertThat(saved.getStatus()).isEqualTo(MatchStatus.SCHEDULED);
    }
}
