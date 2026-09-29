package com.manacommunity.sports.service;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.dto.RegistrationRequest;
import com.manacommunity.sports.dto.SportsEventRequest;
import com.manacommunity.sports.model.SportsEventRegistration;
import com.manacommunity.sports.model.SportsEvent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * BUG FIX: SportsEventService was a stub. SportsController calls:
 * - createEvent, updateStatus, getOpenEvents, getMyEvents, registerUser, withdraw
 * All must be declared here so the controller compiles.
 */
public interface SportsEventService {
    SportsEvent createEvent(SportsEventRequest req, Long adminUserId);
    SportsEvent updateStatus(Long eventId, String status);
    List<SportsEvent> getOpenEvents(Long communityId);
    List<SportsEvent> getMyEvents(Long userId);
    List<SportsEvent> getAllOpenEvents();
    List<SportsEvent> getClosedEvents();
    List<SportsEvent> getClosedEvents(Long communityId);
    SportsEventRegistration registerUser(RegistrationRequest req, Long userId);
    List<SportsEvent> getAllEvents();
    Page<SportsEvent> getAllEvents(Pageable pageable);
    List<SportsEvent> getAllEventsIncludingInactive();
    List<SportsEvent> getCommunityEvents(Long communityId);
    Page<SportsEvent> getCommunityEvents(Long communityId, Pageable pageable);
    List<SportsEvent> getCommunityEventsIncludingInactive(Long communityId);
    SportsEvent getEventById(Long id);
    SportsEvent getEventByUuid(java.util.UUID uuid);
    SportsEvent saveEvent(SportsEvent event);
    void deleteEvent(Long eventId);
    SportsEvent updateEvent(Long eventId, SportsEventRequest req);
    void withdraw(Long registrationId, Long userId);
    List<SportsEventRegistration> getEventRegistrations(Long eventId);
    List<SportsEventRegistration> getUserRegistrations(Long userId);
    SportsEventRegistration confirmRegistration(Long registrationId);
    SportsEventRegistration rejectRegistration(Long registrationId, String reason);
    SportsEventRegistration nominateCaptain(Long registrationId, boolean nominate, String teamName);
    SportsEventRegistration confirmCaptain(Long registrationId, boolean confirm);
    
    java.util.List<java.util.Map<String, Object>> getEventMap(Long communityId);
    long getConfirmedRegistrationCount(Long eventId);
    SportsEvent updateDisputeCommittee(Long eventId, java.util.List<Long> userIds);
}

