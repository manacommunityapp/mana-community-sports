package com.manacommunity.sports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TournamentRequest {
    @NotBlank
    private String name;
    
    @NotNull
    private Long communityId;
    
    @NotNull
    private LocalDate eventDateStart;
    
    @NotNull
    private LocalDate eventDateEnd;
    
    private LocalDate registrationDateStart;
    private LocalDate registrationDateEnd;

    private String startTime;
    private String dueTime;

    @Min(2)
    private Integer maxParticipants;
    
    private List<NotificationScheduleDto> notifications;

    private List<Long> sportsEventIds;
    private List<SponsorDto> sponsors;

    private String contactName;
    private String contactNumber;
    private String contactEmail;
    private String contactTitle;
    private java.util.List<ContactDto> contacts;
    private String otherContacts;
    private String bannerImage;
    private String tournamentLevel;
    private String description;
    private Boolean allowAdminChat;

    

}
