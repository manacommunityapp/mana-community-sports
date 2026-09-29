package com.manacommunity.sports.service.impl;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.dto.ContactDto;
import com.manacommunity.sports.dto.VenueRequest;
import com.manacommunity.sports.dto.VenueResponse;
import com.manacommunity.common.exception.ResourceNotFoundException;
import com.manacommunity.common.model.Community;
import com.manacommunity.sports.model.Contact;
import com.manacommunity.sports.model.Court;
import com.manacommunity.sports.model.Venue;
import com.manacommunity.sports.repository.CommunityRepository;
import com.manacommunity.sports.repository.ContactRepository;
import com.manacommunity.sports.repository.VenueRepository;
import com.manacommunity.sports.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepo;
    private final CommunityRepository communityRepo;
    private final ContactRepository contactRepository;

    private List<Contact> resolveContacts(List<ContactDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return new ArrayList<>();
        return dtos.stream().map(dto -> {
            if (dto.getId() != null) {
                return contactRepository.findById(dto.getId())
                        .orElseGet(() -> contactRepository.save(toContactEntity(dto)));
            }
            return contactRepository.findByNameAndNumberAndEmail(dto.getName(), dto.getNumber(), dto.getEmail())
                    .orElseGet(() -> contactRepository.save(toContactEntity(dto)));
        }).collect(Collectors.toList());
    }

    private Contact toContactEntity(ContactDto dto) {
        return Contact.builder()
                .name(dto.getName())
                .title(dto.getTitle())
                .number(dto.getNumber())
                .email(dto.getEmail())
                .build();
    }

    private ContactDto toContactDto(Contact c) {
        return ContactDto.builder()
                .id(c.getId())
                .name(c.getName())
                .title(c.getTitle())
                .number(c.getNumber())
                .email(c.getEmail())
                .build();
    }

    @Override
    public List<VenueResponse> getVenuesByCommunityId(Long communityId) {
        return venueRepo.findByCommunityIdOrCommunityIdIsNull(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<VenueResponse> getAllVenues() {
        return venueRepo.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public Venue getVenueById(Long id) {
        return venueRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", id));
    }

    @Override
    public VenueResponse getVenueResponseById(Long id) {
        return toResponse(getVenueById(id));
    }

    @Override
    @Transactional
    public VenueResponse createVenue(Long communityId, VenueRequest request) {
        List<Contact> contacts = resolveContacts(request.getContacts());
        Venue venue = Venue.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .area(request.getArea())
                .pinCode(request.getPinCode())
                .mapLink(request.getMapLink())
                .capacity(request.getCapacity())
                .venueType(request.getVenueType())
                .venueCategory(request.getVenueCategory())
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .contactName(request.getContactName())
                .contactNumber(request.getContactNumber())
                .contactEmail(request.getContactEmail())
                .contactTitle(request.getContactTitle())
                .contacts(contacts)
                .build();

        if (communityId != null && communityId > 0) {
            Community community = communityRepo.findById(communityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));
            venue.setCommunity(community);
        }
        applyCourts(venue, request.getCourts());
        return toResponse(venueRepo.save(venue));
    }

    @Override
    @Transactional
    public VenueResponse updateVenue(Long id, VenueRequest request) {
        Venue existing = venueRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", id));
        List<Contact> contacts = resolveContacts(request.getContacts());
        existing.setName(request.getName());
        existing.setAddress(request.getAddress());
        existing.setCity(request.getCity());
        existing.setArea(request.getArea());
        existing.setPinCode(request.getPinCode());
        existing.setMapLink(request.getMapLink());
        existing.setCapacity(request.getCapacity());
        existing.setVenueType(request.getVenueType());
        existing.setVenueCategory(request.getVenueCategory());
        existing.setOpeningTime(request.getOpeningTime());
        existing.setClosingTime(request.getClosingTime());
        existing.setContactName(request.getContactName());
        existing.setContactNumber(request.getContactNumber());
        existing.setContactEmail(request.getContactEmail());
        existing.setContactTitle(request.getContactTitle());
        existing.getContacts().clear();
        existing.getContacts().addAll(contacts);

        existing.getCourts().clear();
        applyCourts(existing, request.getCourts());

        return toResponse(venueRepo.save(existing));
    }

    @Override
    @Transactional
    public void deleteVenue(Long id) {
        if (!venueRepo.existsById(id)) {
            throw new ResourceNotFoundException("Venue", id);
        }
        venueRepo.deleteById(id);
    }

    private void applyCourts(Venue venue, List<VenueRequest.CourtDto> courtDtos) {
        if (courtDtos == null) return;
        for (VenueRequest.CourtDto dto : courtDtos) {
            Court court = Court.builder()
                    .name(dto.getName())
                    .color(dto.getColor())
                    .openingTime(dto.getOpeningTime())
                    .closingTime(dto.getClosingTime())
                    .venue(venue)
                    .build();
            venue.getCourts().add(court);
        }
    }

    private VenueResponse toResponse(Venue venue) {
        return VenueResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .city(venue.getCity())
                .area(venue.getArea())
                .pinCode(venue.getPinCode())
                .mapLink(venue.getMapLink())
                .capacity(venue.getCapacity())
                .venueType(venue.getVenueType())
                .venueCategory(venue.getVenueCategory())
                .openingTime(venue.getOpeningTime())
                .closingTime(venue.getClosingTime())
                .contactName(venue.getContactName())
                .contactNumber(venue.getContactNumber())
                .contactEmail(venue.getContactEmail())
                .contactTitle(venue.getContactTitle())
                .contacts(venue.getContacts() != null
                        ? venue.getContacts().stream().map(this::toContactDto).toList()
                        : List.of())
                .communityId(venue.getCommunity() != null ? venue.getCommunity().getId() : null)
                .communityName(venue.getCommunity() != null ? venue.getCommunity().getName() : null)
                .courts(venue.getCourts() != null
                        ? venue.getCourts().stream().map(c -> VenueResponse.CourtDto.builder()
                            .id(c.getId())
                            .name(c.getName())
                            .color(c.getColor())
                            .openingTime(c.getOpeningTime())
                            .closingTime(c.getClosingTime())
                            .build()).toList()
                        : List.of())
                .build();
    }
}


