package com.manacommunity.sports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for creating/updating a sports event.
 * Enriched with all additional fields required by the frontend/service layers.
 */
@Data
public class SportsEventRequest {
    @NotBlank
    private String name;
    
    @NotNull
    private Long sportId;
    
    @NotNull
    private Long communityId;
    
    @NotNull
    private LocalDate eventDateStart;
    
    @NotNull
    private LocalDate eventDateEnd;
    
    private LocalDate registrationDateStart;
    private LocalDate registrationDateEnd;
    private Long venueId;
    
    @Min(2)
    private Integer maxParticipants;
    
    private String format;
    private String tournamentType;
    private List<Long> categoryIds;
    private List<NotificationScheduleDto> notifications;
    private List<Long> disputeCommitteeIds;
    
    private Integer minPlayers;
    private Integer maxPlayers;
    private String gender;
    private LocalDate playersBorn;
    private List<SponsorDto> sponsors;

    private String contactName;
    private String contactNumber;
    private String contactEmail;
    private String contactTitle;
    private java.util.List<ContactDto> contacts;
    private String otherContacts;
    private Boolean auctionEnabled;
    private String bannerImage;
    private String tournamentLevel;
    private String description;
    private Boolean allowAdminChat;
    private String startTime;
    private String dueTime;
    
    private Integer minAge;
    private Integer maxAge;

    /**
     * When true (default), self-registrations land as PENDING and need organiser
     * confirmation; when false they auto-confirm. Null is treated as true.
     */
    private Boolean adminApprovalRequired;

    private Long tournamentId;
}
