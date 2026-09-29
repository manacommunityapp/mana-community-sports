package com.manacommunity.sports.repository;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.SportsMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * BUG FIX: Repository was missing entirely. SportsEventServiceImpl and
 * SportsController both inject SportMetaRepository but no such interface
 * existed, causing a Spring context startup failure.
 */
@Repository
public interface SportMetaRepository extends JpaRepository<SportsMeta, Long> {
    /** Used by SportsController.getAllSports() for dropdown population. */
    List<SportsMeta> findByActiveTrue();

    @org.springframework.data.jpa.repository.Query(
        "SELECT s FROM SportsMeta s WHERE s.active = true AND (s.communityId IS NULL OR s.communityId = :communityId)")
    List<SportsMeta> findActiveByCommunity(@org.springframework.data.repository.query.Param("communityId") Long communityId);

    java.util.Optional<SportsMeta> findByNameIgnoreCase(String name);
}

