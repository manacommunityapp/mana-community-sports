package com.manacommunity.sports.service.sample.data;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.SportsMeta;
import com.manacommunity.sports.repository.SportMetaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SportsMetaSeeder - Handles database seeding for globally registered
 * sports metadata in the sports_meta table.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsMetaSeeder {

    private final SportMetaRepository sportRepo;

    @Transactional
    public void defaultSeed() {
        log.info("Starting sports meta database seeding...");
        SportsMeta cricket   = getOrCreateSport("Cricket",   "🏏");
        SportsMeta football  = getOrCreateSport("Football",  "⚽");
        SportsMeta badminton = getOrCreateSport("Badminton", "🏸");

        getOrCreateSport("Basketball", "🏀");
        getOrCreateSport("Beach Volleyball", "🏐");
        getOrCreateSport("Billiards", "🎱");
        getOrCreateSport("Bowling", "🎳");
        getOrCreateSport("Carrom", "🔴");
        getOrCreateSport("Chess", "♟️");
        getOrCreateSport("Cricket (Tennis Ball)", "🏏");
        getOrCreateSport("Cycling", "🚴");
        getOrCreateSport("Dart", "🎯");
        getOrCreateSport("Foosball", "⚽");
        getOrCreateSport("Grass Volleyball", "🏐");
        getOrCreateSport("Kabaddi", "🤼");
        getOrCreateSport("Pickleball", "🏓");
        getOrCreateSport("Pool", "🎱");
        getOrCreateSport("Running (100M)", "🏃");
        getOrCreateSport("Running (1500M)", "🏃");
        getOrCreateSport("Running (200M)", "🏃");
        getOrCreateSport("Running (400M)", "🏃");
        getOrCreateSport("Running (800M)", "🏃");
        getOrCreateSport("Running (Others)", "🏃");
        getOrCreateSport("Snooker", "🎱");
        getOrCreateSport("Soccer", "⚽");
        getOrCreateSport("Squash", "🎾");
        getOrCreateSport("Swimming Race", "🏊");
        getOrCreateSport("Table Tennis", "🏓");
        getOrCreateSport("Tennis", "🎾");
        getOrCreateSport("Throwball", "🤾");
        getOrCreateSport("Tug of War", "🪢");
        getOrCreateSport("Volleyball", "🏐");

        log.info("✓ Sports seeded: Cricket (id={}), Football (id={}), Badminton (id={}) and 27 other sports",
                cricket.getId(), football.getId(), badminton.getId());
    }

    public SportsMeta getOrCreateSport(String name, String icon) {
        String format = isTeamSportName(name) ? "TEAM" : "SINGLES";
        return getOrCreateSport(name, icon, format);
    }

    public SportsMeta getOrCreateSport(String name, String icon, String format) {
        return sportRepo.findByNameIgnoreCase(name).orElseGet(() -> {
            SportsMeta s = new SportsMeta();
            s.setName(name);
            s.setIcon(icon);
            s.setFormats(List.of(format));
            s.setActive(true);
            return sportRepo.save(s);
        });
    }

    private boolean isTeamSportName(String sportName) {
        String name = sportName.toLowerCase();
        return name.contains("cricket") ||
               name.contains("football") ||
               name.contains("volleyball") ||
               name.contains("basketball") ||
               name.contains("kabaddi") ||
               name.contains("hockey") ||
               name.contains("soccer") ||
               name.contains("throwball") ||
               name.contains("rugby");
    }
}

