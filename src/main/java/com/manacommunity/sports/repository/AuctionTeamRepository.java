package com.manacommunity.sports.repository;

import com.manacommunity.sports.model.AuctionTeam;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuctionTeamRepository extends JpaRepository<AuctionTeam, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM AuctionTeam t WHERE t.id = :id")
    java.util.Optional<AuctionTeam> findByIdForUpdate(@Param("id") Long id);
    List<AuctionTeam> findByConfigIdOrderByTeamName(Long configId);
    long countByConfigId(Long configId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(t.totalBudget), 0) FROM AuctionTeam t WHERE t.config.id = :configId")
    long sumBudgetByConfigId(@org.springframework.data.repository.query.Param("configId") Long configId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(t.spent), 0) FROM AuctionTeam t WHERE t.config.id = :configId")
    long sumSpentByConfigId(@org.springframework.data.repository.query.Param("configId") Long configId);

    java.util.Optional<AuctionTeam> findByConfigIdAndOwnerUserId(Long configId, Long ownerUserId);
    List<AuctionTeam> findByConfigIdAndCaptainNominationTrue(Long configId);
    List<AuctionTeam> findByOwnerUserIdOrCaptainUserId(Long ownerUserId, Long captainUserId);
}
