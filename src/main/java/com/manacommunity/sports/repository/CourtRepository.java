package com.manacommunity.sports.repository;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.Court;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourtRepository extends JpaRepository<Court, Long> {
    List<Court> findByVenueId(Long venueId);
}

