package com.manacommunity.sports.repository;

import com.manacommunity.sports.model.AuctionBid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {

    /** Highest bid for a player — used to determine current bid */
    @Query("SELECT MAX(b.bidAmount) FROM AuctionBid b WHERE b.player.id = :pid")
    Optional<Long> findMaxBidForPlayer(@Param("pid") Long playerId);

    /** Full bid history for a player, newest first */
    List<AuctionBid> findByPlayerIdOrderByBidAtDesc(Long playerId);

    /** Last bid entry — tells us which team currently leads */
    @Query("SELECT b FROM AuctionBid b WHERE b.player.id = :pid ORDER BY b.bidAmount DESC")
    List<AuctionBid> findTopBidsForPlayer(@Param("pid") Long playerId, Pageable pageable);
    
    // Existing method mapping
    @Query("SELECT MAX(b.bidAmount) FROM AuctionBid b WHERE b.player.id = :playerId")
    Integer findMaxBidByPlayer(@Param("playerId") Long playerId);
}
