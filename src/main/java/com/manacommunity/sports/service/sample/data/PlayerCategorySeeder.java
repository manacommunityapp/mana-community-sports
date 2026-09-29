package com.manacommunity.sports.service.sample.data;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;
import com.manacommunity.sports.model.PlayerCategory;
import com.manacommunity.sports.repository.PlayerCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * PlayerCategorySeeder — Seeds baseline player categories linked to communities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerCategorySeeder {

    private final PlayerCategoryRepository playerCategoryRepo;
    private final CommunitySeeder communitySeeder;

    @Transactional
    public void defaultSeed() {
        log.info("Seeding player categories...");
        Community generalCommunity = communitySeeder.getGeneralCommunity();

        getOrCreatePlayerCategory("Boy's Under 19",    "BOYS",    "MALE",    0, 9,  generalCommunity);
        getOrCreatePlayerCategory("Men's Above 19",    "MENS",    "MALE",   19, 45, generalCommunity);
        getOrCreatePlayerCategory("Women's Above 19",  "WOMENS",  "FEMALE", 18, 50, generalCommunity);
        getOrCreatePlayerCategory("Girl's Under 19",   "GIRLS",   "FEMALE",  0, 19, generalCommunity);
        getOrCreatePlayerCategory("Kid's Under 12",    "KIDS",    "ALL",     5, 12, generalCommunity);
        getOrCreatePlayerCategory("Senior's Above 45", "SENIORS", "ALL",    45, 55, generalCommunity);

        log.info("✓ Player categories seeded successfully.");
    }

    @Transactional
    public void seed() {
        log.info("Seeding player categories...");
        Community generalCommunity = communitySeeder.getGeneralCommunity();

        getOrCreatePlayerCategory("Boy's Under 19",    "BOYS",    "MALE",    0, 9,  generalCommunity);
        getOrCreatePlayerCategory("Men's Above 19",    "MENS",    "MALE",   19, 45, generalCommunity);
        getOrCreatePlayerCategory("Women's Above 19",  "WOMENS",  "FEMALE", 18, 50, generalCommunity);
        getOrCreatePlayerCategory("Girl's Under 19",   "GIRLS",   "FEMALE",  0, 19, generalCommunity);
        getOrCreatePlayerCategory("Kid's Under 12",    "KIDS",    "ALL",     5, 12, generalCommunity);
        getOrCreatePlayerCategory("Senior's Above 45", "SENIORS", "ALL",    45, 55, generalCommunity);

        log.info("✓ Player categories seeded successfully.");
    }

    public PlayerCategory getCategoryByName(String name) {
        return playerCategoryRepo.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PlayerCategory " + name + " has not been seeded yet."));
    }

    public Set<PlayerCategory> getSummerCupCategories() {
        PlayerCategory boysU19 = getCategoryByName("Boy's Under 19");
        PlayerCategory mensA19 = getCategoryByName("Men's Above 19");
        return Set.of(boysU19, mensA19);
    }

    private PlayerCategory getOrCreatePlayerCategory(String name, String categoryType,
                                                     String gender, int minAge, int maxAge,
                                                     Community community) {
        return playerCategoryRepo.findAll().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseGet(() -> playerCategoryRepo.save(PlayerCategory.builder()
                        .name(name)
                        .category_type(categoryType)
                        .gender(gender)
                        .minAge(minAge)
                        .maxAge(maxAge)
                        .community(community)
                        .type("DEFAULT")
                        .description("Sample " + name + " category")
                        .build()));
    }
}


