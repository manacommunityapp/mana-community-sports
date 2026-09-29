package com.manacommunity.sports.repository;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;
import com.manacommunity.sports.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    @Query("SELECT t FROM Tournament t WHERE t.community.id = :communityId ORDER BY t.createdAt DESC")
    List<Tournament> findByEventCommunityIdOrderByCreatedAtDesc(@Param("communityId") Long communityId);

    @Query("SELECT COUNT(t) > 0 FROM Tournament t JOIN t.sportsEvents se WHERE se.id = :eventId")
    boolean existsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT DISTINCT t FROM Tournament t JOIN t.sportsEvents se WHERE se.id = :eventId")
    List<Tournament> findByEventIdList(@Param("eventId") Long eventId);

    default Optional<Tournament> findByEventId(Long eventId) {
        List<Tournament> list = findByEventIdList(eventId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Query("SELECT DISTINCT t FROM Tournament t LEFT JOIN FETCH t.sportsEvents WHERE t.registrationStatus = :status ORDER BY t.createdAt DESC")
    List<Tournament> findByRegistrationStatusWithEvents(@Param("status") Tournament.EventStatus status);

    @Query("SELECT DISTINCT t FROM Tournament t LEFT JOIN FETCH t.sportsEvents se WHERE t.registrationStatus = :status AND t.community.id = :communityId ORDER BY t.createdAt DESC")
    List<Tournament> findByRegistrationStatusAndCommunityWithEvents(@Param("status") Tournament.EventStatus status, @Param("communityId") Long communityId);

    long countByRegistrationStatusIn(List<Tournament.EventStatus> statuses);
    long countByCommunityIdAndRegistrationStatusIn(Long communityId, List<Tournament.EventStatus> statuses);

    @Modifying
    @Transactional
    @Query("DELETE FROM Tournament t WHERE t.id IN (SELECT t2.id FROM Tournament t2 JOIN t2.sportsEvents se WHERE se.id = :eventId)")
    void deleteByEventId(@Param("eventId") Long eventId);
}


