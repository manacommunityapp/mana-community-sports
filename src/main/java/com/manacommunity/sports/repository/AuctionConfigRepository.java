package com.manacommunity.sports.repository;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;
import com.manacommunity.sports.model.AuctionConfig;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionConfigRepository extends JpaRepository<AuctionConfig, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM AuctionConfig c WHERE c.id = :id")
    Optional<AuctionConfig> findByIdForUpdate(@Param("id") Long id);
    List<AuctionConfig> findBySportIdOrderByCreatedAtDesc(Long sportId);
    List<AuctionConfig> findBySportIdAndCreatedByCommunityIdOrderByCreatedAtDesc(Long sportId, Long communityId);
    List<AuctionConfig> findByCreatedByCommunityIdOrderByCreatedAtDesc(Long communityId);

    @Query("SELECT c FROM AuctionConfig c LEFT JOIN FETCH c.sport LEFT JOIN FETCH c.event "
         + "WHERE c.sport.id = :sportId ORDER BY c.createdAt DESC")
    List<AuctionConfig> findBySportIdWithAssociations(@Param("sportId") Long sportId);

    @Query("SELECT c FROM AuctionConfig c LEFT JOIN FETCH c.sport LEFT JOIN FETCH c.event "
         + "WHERE c.sport.id = :sportId AND c.createdBy.community.id = :communityId ORDER BY c.createdAt DESC")
    List<AuctionConfig> findBySportIdAndCommunityIdWithAssociations(@Param("sportId") Long sportId, @Param("communityId") Long communityId);

    @Query("SELECT c FROM AuctionConfig c LEFT JOIN FETCH c.sport LEFT JOIN FETCH c.event "
         + "WHERE c.createdBy.community.id = :communityId ORDER BY c.createdAt DESC")
    List<AuctionConfig> findByCommunityIdWithAssociations(@Param("communityId") Long communityId);
    Optional<AuctionConfig> findBySportIdAndStatus(Long sportId, AuctionConfig.AuctionStatus status);
    Optional<AuctionConfig> findByEventId(Long eventId);
    boolean existsBySportIdAndSeasonName(Long sportId, String seasonName);
}


