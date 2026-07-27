package com.manacommunity.sports.repository;

import com.manacommunity.sports.model.AuctionConfigCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuctionConfigCategoryRepository extends JpaRepository<AuctionConfigCategory, Long> {
}
