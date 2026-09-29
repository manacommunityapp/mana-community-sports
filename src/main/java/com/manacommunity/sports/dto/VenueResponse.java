package com.manacommunity.sports.dto;

import com.manacommunity.common.enums.*;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VenueResponse {

    private Long id;
    private String name;
    private String address;
    private String city;
    private String area;
    private String pinCode;
    private String mapLink;
    private Integer capacity;
    private String venueType;
    private String venueCategory;
    private String openingTime;
    private String closingTime;
    private String contactName;
    private String contactNumber;
    private String contactEmail;
    private Long contactId;
    private String contactTitle;
    private Long communityId;
    private String communityName;
    private List<ContactDto> contacts;
    private List<CourtDto> courts;

    @Data
    @Builder
    public static class CourtDto {
        private Long id;
        private String name;
        private String color;
        private String openingTime;
        private String closingTime;
    }
}

