package com.manacommunity.sports.service;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.dto.VenueRequest;
import com.manacommunity.sports.dto.VenueResponse;
import com.manacommunity.sports.model.Venue;

import java.util.List;

public interface VenueService {
    List<VenueResponse> getVenuesByCommunityId(Long communityId);
    List<VenueResponse> getAllVenues();
    Venue getVenueById(Long id);
    VenueResponse getVenueResponseById(Long id);
    VenueResponse createVenue(Long communityId, VenueRequest request);
    VenueResponse updateVenue(Long id, VenueRequest request);
    void deleteVenue(Long id);
}

