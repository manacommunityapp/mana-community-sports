package com.manacommunity.sports.repository;

import com.manacommunity.sports.model.SportsEventRegistration;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsEventRegistrationRepository extends JpaRepository<SportsEventRegistration, Long> {
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
    boolean existsByEventIdAndUserIdAndPlayerName(Long eventId, Long userId, String playerName);

    boolean existsByEventIdAndUserIsNullAndPlayerName(Long eventId, String playerName);
    long countByEventId(Long eventId);

    @EntityGraph(attributePaths = {"event", "event.sport", "user", "category", "partner"})
    List<SportsEventRegistration> findByEventId(Long eventId);

    @EntityGraph(attributePaths = {"event", "event.sport", "user", "category", "partner"})
    List<SportsEventRegistration> findByUserId(Long userId);
    List<SportsEventRegistration> findByEventIdAndStatus(Long eventId, SportsEventRegistration.RegistrationStatus status);
    long countByEventIdAndStatus(Long eventId, SportsEventRegistration.RegistrationStatus status);

//    @org.springframework.data.jpa.repository.Query("""
//        SELECT COUNT(r) FROM SportsEventRegistration r
//        WHERE r.event.community.id = :communityId
//          AND r.event.sport.id = :sportId
//          AND r.status = :registrationStatus
//          AND r.event.registrationStatus NOT IN :excludedStatuses
//    """)
    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(r) FROM SportsEventRegistration r 
        WHERE r.event.community.id = :communityId 
          AND r.event.sport.id = :sportId 
          AND r.status = :registrationStatus 
    """)
    long countActiveRegistrationsForCommunityAndSport(
            @org.springframework.data.repository.query.Param("communityId") Long communityId, 
            @org.springframework.data.repository.query.Param("sportId") Long sportId, 
            //@org.springframework.data.repository.query.Param("registrationStatus") SportsEventRegistration.RegistrationStatus status,
            @org.springframework.data.repository.query.Param("excludedStatuses") java.util.List<com.manacommunity.sports.model.SportsEvent.EventStatus> excludedStatuses);
    @org.springframework.data.jpa.repository.Query("SELECT r FROM SportsEventRegistration r WHERE r.event.community.id = :communityId")
    List<SportsEventRegistration> findByCommunityId(@org.springframework.data.repository.query.Param("communityId") Long communityId);
}
