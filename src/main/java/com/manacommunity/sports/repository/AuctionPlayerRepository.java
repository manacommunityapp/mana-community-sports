package com.manacommunity.sports.repository;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.AuctionPlayer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionPlayerRepository extends JpaRepository<AuctionPlayer, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM AuctionPlayer p WHERE p.id = :id")
    Optional<AuctionPlayer> findByIdForUpdate(@Param("id") Long id);

    /** Next player in queue for this auction, excluding team captains and owners */
    @Query("SELECT p FROM AuctionPlayer p WHERE p.config.id = :cid AND p.status = 'QUEUED' " +
           "AND (p.user IS NULL OR (p.user.id NOT IN (SELECT t.captainUser.id FROM AuctionTeam t WHERE t.config.id = :cid AND t.captainUser IS NOT NULL) " +
           "AND p.user.id NOT IN (SELECT t.ownerUser.id FROM AuctionTeam t WHERE t.config.id = :cid AND t.ownerUser IS NOT NULL))) " +
           "ORDER BY p.queueOrder ASC")
    List<AuctionPlayer> findQueuedByConfig(@Param("cid") Long configId);

    /** Player currently being sold, excluding team captains and owners */
    @Query("SELECT p FROM AuctionPlayer p WHERE p.config.id = :cid AND p.status = 'SELLING' " +
           "AND (p.user IS NULL OR (p.user.id NOT IN (SELECT t.captainUser.id FROM AuctionTeam t WHERE t.config.id = :cid AND t.captainUser IS NOT NULL) " +
           "AND p.user.id NOT IN (SELECT t.ownerUser.id FROM AuctionTeam t WHERE t.config.id = :cid AND t.ownerUser IS NOT NULL)))")
    Optional<AuctionPlayer> findSellingPlayer(@Param("cid") Long configId);

    @Query("SELECT COUNT(p) FROM AuctionPlayer p WHERE p.config.id = :cid AND p.status = 'QUEUED' " +
           "AND (p.user IS NULL OR (p.user.id NOT IN (SELECT t.captainUser.id FROM AuctionTeam t WHERE t.config.id = :cid AND t.captainUser IS NOT NULL) " +
           "AND p.user.id NOT IN (SELECT t.ownerUser.id FROM AuctionTeam t WHERE t.config.id = :cid AND t.ownerUser IS NOT NULL)))")
    long countQueuedByConfig(@Param("cid") Long configId);

    @Query("SELECT COUNT(p) FROM AuctionPlayer p WHERE p.config.id = :cid AND p.status = 'SOLD'")
    int countSold(@Param("cid") Long configId);

    @EntityGraph(attributePaths = {"assignedTeam", "user"})
    List<AuctionPlayer> findByConfigIdAndCategoryOrderByQueueOrder(Long configId, String category);
    long countByConfigId(Long configId);
    long countByConfigIdAndStatus(Long configId, AuctionPlayer.PlayerStatus status);

    @EntityGraph(attributePaths = {"assignedTeam", "user"})
    List<AuctionPlayer> findByConfigId(Long configId);

    @EntityGraph(attributePaths = {"assignedTeam", "user"})
    List<AuctionPlayer> findByConfigIdAndStatusOrderByQueueOrderAsc(Long configId, AuctionPlayer.PlayerStatus status);
}

