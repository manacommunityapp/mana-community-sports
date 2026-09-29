package com.manacommunity.sports.service;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.dto.schedule.SportsScheduleResponse.EventListItem;
import com.manacommunity.sports.dto.schedule.SportsScheduleResponse.RegistrationListItem;
import com.manacommunity.sports.model.PlayerCategory;
import com.manacommunity.sports.model.SportsEvent;
import com.manacommunity.sports.model.SportsEventRegistration;
import com.manacommunity.common.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SportsScheduleService {

    private final SportsEventService eventService;

    @Transactional(readOnly = true)
    public List<EventListItem> getOpenEvents(Long communityId) {
        return eventService.getOpenEvents(communityId).stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventListItem> getMyEvents(Long userId) {
        return eventService.getMyEvents(userId).stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventListItem> getAllOpenEvents() {
        return eventService.getAllOpenEvents().stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrationListItem> getMyRegistrations(Long userId) {
        return eventService.getUserRegistrations(userId).stream()
                .map(this::toRegistrationItem)
                .toList();
    }

    private EventListItem toListItem(SportsEvent e) {
        String sportName = e.getSport() != null ? e.getSport().getName() : null;
        String venueName = e.getVenue() != null ? e.getVenue().getName() : null;
        String venueCity = e.getVenue() != null ? e.getVenue().getCity() : null;
        String categoryName = firstCategoryName(e.getCategories());
        String status = e.getStatus() != null ? e.getStatus().name() : null;
        String auctionStatus = e.getAuctionStatus() != null ? e.getAuctionStatus().name() : null;

        return new EventListItem(
                e.getId(),
                e.getUuid() != null ? e.getUuid().toString() : null,
                e.getName(),
                sportName,
                venueName,
                venueCity,
                e.getEventDateStart(),
                e.getEventDateEnd(),
                status,
                auctionStatus,
                e.getFormat(),
                categoryName,
                e.getMaxParticipants(),
                e.getStartTime()
        );
    }

    private RegistrationListItem toRegistrationItem(SportsEventRegistration r) {
        SportsEvent event = r.getEvent();
        String eventName = event != null ? event.getName() : null;
        Long eventId = event != null ? event.getId() : null;
        String sportName = (event != null && event.getSport() != null) ? event.getSport().getName() : null;
        String categoryName = r.getCategory() != null ? r.getCategory().getName() : null;

        return new RegistrationListItem(
                r.getId(),
                r.getStatus() != null ? r.getStatus().name() : null,
                eventId,
                eventName,
                sportName,
                categoryName,
                r.getAge(),
                r.getFlatNumber(),
                r.getRegisteredAt(),
                r.getCaptainNomination(),
                r.getCaptainConfirmation(),
                r.getProposedTeamName()
        );
    }

    private String firstCategoryName(Set<PlayerCategory> categories) {
        if (categories == null || categories.isEmpty()) return null;
        return categories.iterator().next().getName();
    }
}


