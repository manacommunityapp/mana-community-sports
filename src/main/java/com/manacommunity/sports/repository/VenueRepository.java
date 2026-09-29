package com.manacommunity.sports.repository;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;
import com.manacommunity.sports.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {
    @Query("SELECT v FROM Venue v LEFT JOIN v.community c WHERE c.id = :communityId OR c.id IS NULL")
    List<Venue> findByCommunityIdOrCommunityIdIsNull(@Param("communityId") Long communityId);
}


