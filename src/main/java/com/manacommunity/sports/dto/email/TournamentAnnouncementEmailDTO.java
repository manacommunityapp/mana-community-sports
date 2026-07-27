package com.manacommunity.sports.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentAnnouncementEmailDTO {
    private String appName;
    private String tournamentName;
    private String tournamentDescription;
    private String organizerName;
    private String venueName;
    private String venueAddress;
    private String eventStartDate;
    private String eventEndDate;
    private String registrationStartDate;
    private String registrationEndDate;
    private String announcementDate;
    private String openingCeremonyDate;
    private String fixturesDate;
    private String finalsDate;
    private Integer expectedParticipants;
    private Integer totalSports;
    private Integer totalEvents;
    private String customMessage;
    private String actionButtonText;
    private String actionUrl;
    private String supportEmail;
    private String supportPhone;
    private String footerText;
    private String logoUrl;
    private String bannerImage;
    private Integer year;

    @Builder.Default
    private List<SportEventDTO> sportsEvents = new ArrayList<>();

    @Builder.Default
    private List<AnnouncementDTO> announcements = new ArrayList<>();

    @Builder.Default
    private List<TimelineDTO> timeline = new ArrayList<>();

    @Builder.Default
    private List<SportDTO> sportsIncluded = new ArrayList<>();

    @Builder.Default
    private List<GalleryDTO> galleryImages = new ArrayList<>();
}
