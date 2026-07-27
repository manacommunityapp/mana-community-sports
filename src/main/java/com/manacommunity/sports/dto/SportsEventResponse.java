package com.manacommunity.sports.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SportsEventResponse {
    private Long id;
    private UUID uuid;
    private String name;
    private String icon;
    private Integer minAge;
    private Integer maxAge;
    private Integer minPlayers;
    private Integer maxPlayers;
    private String gender;
    private LocalDate playersBorn;
    private Boolean active;
    private Boolean adminApprovalRequired;

    private SportRef sport;
    private CommunityRef community;
    private VenueRef venue;
    private CreatedByRef createdBy;

    private LocalDate eventDateStart;
    private LocalDate eventDateEnd;
    private LocalDate registrationDateStart;
    private LocalDate registrationDateEnd;
    private Integer maxParticipants;
    private String startTime;
    private String dueTime;

    private String status;
    private String auctionStatus;
    private List<String> format;
    private String tournamentType;

    private List<CategoryRef> categories;
    private List<SponsorDto> sponsors;

    private String contactName;
    private String contactNumber;
    private String contactEmail;
    private Long contactId;
    private String contactTitle;
    private java.util.List<ContactDto> contacts;
    private String otherContacts;
    private Boolean auctionEnabled;
    private String bannerImage;
    private String tournamentLevel;
    private String description;
    private java.util.List<Long> disputeCommitteeIds;

    private TournamentRef tournament;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class SportRef {
        private Long id;
        private String name;
        private String icon;
        private String iconUrl;
    }

    @Data
    @Builder
    public static class CommunityRef {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    public static class VenueRef {
        private Long id;
        private String name;
        private String address;
        private String city;
        private String area;
    }

    @Data
    @Builder
    public static class CreatedByRef {
        private Long id;
        private String name;
        private String email;
    }

    @Data
    @Builder
    public static class CategoryRef {
        private Long id;
        private String name;
        private String categoryType;
        private String description;
        private Integer minAge;
        private Integer maxAge;
        private String gender;
        private String type;
    }

    @Data
    @Builder
    public static class TournamentRef {
        private Long id;
        private String name;
        private String registrationStatus;
    }
}
