package com.manacommunity.sports.repository;

import com.manacommunity.sports.model.PlayerCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * BUG FIX: Repository was missing entirely. SportsEventServiceImpl and
 * SportsController both inject PlayerCategoryRepository but no such
 * interface existed, causing a Spring context startup failure.
 */
@Repository
public interface PlayerCategoryRepository extends JpaRepository<PlayerCategory, Long> {

    /** Find categories belonging to a specific community */
    List<PlayerCategory> findByCommunityId(Long communityId);

    java.util.Optional<PlayerCategory> findByNameIgnoreCase(String name);

    /**
     * For non-super-admin users: returns DEFAULT type categories (visible to everyone)
     * PLUS categories belonging to the user's community.
     */
    @Query("SELECT c FROM PlayerCategory c WHERE c.type = 'DEFAULT' OR c.community.id = :communityId")
    List<PlayerCategory> findDefaultAndCommunityCategories(@Param("communityId") Long communityId);
}

