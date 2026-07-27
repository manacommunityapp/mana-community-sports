package com.manacommunity.sports.repository;

import com.manacommunity.sports.model.AuctionDisputeCommittee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuctionDisputeCommitteeRepository extends JpaRepository<AuctionDisputeCommittee, Long> {
}
