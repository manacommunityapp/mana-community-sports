package com.manacommunity.sports.repository;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.AuctionSessionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuctionSessionLogRepository extends JpaRepository<AuctionSessionLog, Long> {
}

