package com.manacommunity.sports.service.sample.data;

import com.manacommunity.common.model.Community;
import com.manacommunity.sports.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunitySeeder {

    private final CommunityRepository communityRepository;

    public Community getManaApartments() {
        return communityRepository.findAll().stream().findFirst().orElse(null);
    }

    public Community getLeCommunity() {
        return getManaApartments();
    }

    public Community getGeneralCommunity() {
        return getManaApartments();
    }
}
