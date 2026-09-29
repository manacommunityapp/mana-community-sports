package com.manacommunity.sports.email;

import com.manacommunity.common.util.EmailTemplateRenderer;
import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;
import com.manacommunity.sports.dto.email.*;
import com.manacommunity.sports.repository.TournamentRepository;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.user.repository.AppUserRepository;
import com.manacommunity.sports.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles sending general tournament announcement emails (e.g. registration open announcements).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentEmailService {

    private final EmailSupport support;
    private final EmailTemplateRenderer renderer;
    private final EmailService emailService;
    private final AppUserRepository appUserRepository;
    private final TournamentRepository tournamentRepository;
    private final com.manacommunity.sports.repository.TournamentAnnouncementRepository announcementRepository;
    private final com.manacommunity.sports.repository.TournamentGalleryImageRepository galleryRepository;
    private final com.manacommunity.sports.repository.TournamentTimelineEntryRepository timelineRepository;

    @Transactional(readOnly = true)
    public TournamentAnnouncementEmailDTO getTournamentAnnouncement(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new com.manacommunity.common.exception.ResourceNotFoundException("Tournament", tournamentId));
        return buildTournamentAnnouncementDTO(tournament, null);
    }

    /** Renders the announcement email to HTML for the given tournament (used by the preview endpoint). */
    @Transactional(readOnly = true)
    public String renderAnnouncementHtml(Long tournamentId) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("email", getTournamentAnnouncement(tournamentId));
        return renderer.render(EmailTemplate.TOURNAMENT_ANNOUNCEMENT, vars);
    }

    public TournamentAnnouncementEmailDTO buildTournamentAnnouncementDTO(Tournament tournament, String customMessageOverride) {
        if (tournament == null) return TournamentAnnouncementEmailDTO.builder().build();

        String appName = support.props() != null && support.props().getFromName() != null ? support.props().getFromName() : "Mana Community";
        String regStartStr = support.formatDate(tournament.getRegistrationDateStart());
        String regEndStr = support.formatDate(tournament.getRegistrationDateEnd());
        String evtStartStr = support.formatDate(tournament.getEventDateStart());
        String evtEndStr = support.formatDate(tournament.getEventDateEnd());
        String createdStr = support.formatDate(tournament.getCreatedAt() != null ? tournament.getCreatedAt().toLocalDate() : null);

        // Sub-events
        List<SportEventDTO> sportsEvents = new ArrayList<>();
        List<SportDTO> sportsIncluded = new ArrayList<>();
        String venueName = "Community Sports Arena";

        if (tournament.getSportsEvents() != null && !tournament.getSportsEvents().isEmpty()) {
            for (SportsEvent e : tournament.getSportsEvents()) {
                String sName = e.getSport() != null ? e.getSport().getName() : (e.getName() != null ? e.getName() : "Sport");
                String eName = e.getName() != null ? e.getName() : sName;
                String icon = e.getIcon() != null ? e.getIcon() : (e.getSport() != null && e.getSport().getIcon() != null ? e.getSport().getIcon() : "🏅");
                String gender = e.getGender() != null ? e.getGender() : "All Categories";
                String ageRange = e.getMinAge() != null && e.getMaxAge() != null ? (e.getMinAge() + "–" + e.getMaxAge() + " Yrs") : "Open Category";

                sportsEvents.add(SportEventDTO.builder()
                        .eventName(eName)
                        .sportName(sName)
                        .icon(icon)
                        .gender(gender)
                        .ageRange(ageRange)
                        .eventDate(!evtStartStr.isBlank() ? evtStartStr : "Upcoming")
                        .venueName(e.getVenue() != null ? e.getVenue().getName() : venueName)
                        .iconBgColor(sportColor(sName))
                        .build());
                
                if (sportsIncluded.stream().noneMatch(s -> s.getSportName().equalsIgnoreCase(sName))) {
                    sportsIncluded.add(SportDTO.builder().sportName(sName).icon(icon).build());
                }

                if (e.getVenue() != null && e.getVenue().getName() != null) {
                    venueName = e.getVenue().getName();
                }
            }
        }

        // Timeline milestones — prefer custom DB entries; else derive from the
        // tournament's own registration/event dates.
        List<TimelineDTO> timeline = new ArrayList<>();
        List<com.manacommunity.sports.model.TournamentTimelineEntry> timelineRows = tournament.getId() != null
                ? timelineRepository.findByTournamentIdOrderBySortOrderAscIdAsc(tournament.getId())
                : List.of();
        if (!timelineRows.isEmpty()) {
            for (var t : timelineRows) {
                String dateStr = t.getDateLabel() != null && !t.getDateLabel().isBlank()
                        ? t.getDateLabel() : support.formatDate(t.getEntryDate());
                timeline.add(TimelineDTO.builder()
                        .date(dateStr != null && !dateStr.isBlank() ? dateStr : "TBA")
                        .title(t.getTitle())
                        .description(t.getDescription())
                        .build());
            }
        } else {
            timeline.add(TimelineDTO.builder().date(!createdStr.isBlank() ? createdStr : "Published").title("Tournament Announcements").build());
            timeline.add(TimelineDTO.builder().date(!regStartStr.isBlank() ? regStartStr : "Open").title("Registration Starts").build());
            timeline.add(TimelineDTO.builder().date(!regEndStr.isBlank() ? regEndStr : "Closing Soon").title("Registration Ends").build());
            timeline.add(TimelineDTO.builder().date(!regEndStr.isBlank() ? regEndStr : "Published").title("Fixtures Published").build());
            timeline.add(TimelineDTO.builder().date(!evtStartStr.isBlank() ? (evtStartStr + " " + (tournament.getStartTime() != null ? tournament.getStartTime() : "09:00 AM")) : "Day 1 09:00 AM").title("Opening Ceremony & Start").build());
            timeline.add(TimelineDTO.builder().date(!evtEndStr.isBlank() ? evtEndStr : "Final Day").title("Finals & Prizes").build());
        }

        // Announcements — prefer custom DB entries; else sensible defaults.
        List<AnnouncementDTO> announcements = new ArrayList<>();
        List<com.manacommunity.sports.model.TournamentAnnouncement> annRows = tournament.getId() != null
                ? announcementRepository.findByTournamentIdOrderBySortOrderAscIdAsc(tournament.getId())
                : List.of();
        if (!annRows.isEmpty()) {
            for (var a : annRows) {
                announcements.add(AnnouncementDTO.builder()
                        .icon(a.getIcon() != null ? a.getIcon() : "📢")
                        .title(a.getTitle())
                        .content(a.getContent())
                        .date(support.formatDate(a.getCreatedAt() != null ? a.getCreatedAt().toLocalDate() : null))
                        .build());
            }
        } else {
            announcements.add(AnnouncementDTO.builder().icon("📢").content("Match fixtures will be published shortly before opening.").build());
            announcements.add(AnnouncementDTO.builder().icon("📢").content("Registrations are open on a first-come, first-served basis.").build());
        }

        // Gallery — prefer custom DB images; else placeholder tiles.
        List<GalleryDTO> gallery = new ArrayList<>();
        List<com.manacommunity.sports.model.TournamentGalleryImage> galleryRows = tournament.getId() != null
                ? galleryRepository.findByTournamentIdOrderBySortOrderAscIdAsc(tournament.getId())
                : List.of();
        if (!galleryRows.isEmpty()) {
            for (var g : galleryRows) {
                gallery.add(GalleryDTO.builder()
                        .title(g.getTitle())
                        .icon(g.getIcon() != null ? g.getIcon() : "🏆")
                        .bgColor(g.getBgColor() != null ? g.getBgColor() : "#2563eb")
                        .imageUrl(g.getImageUrl())
                        .build());
            }
        } else {
            gallery.add(GalleryDTO.builder().title("Championship").icon("🏆").bgColor("#f59e0b").build());
            gallery.add(GalleryDTO.builder().title("Celebrations").icon("🎉").bgColor("#2563eb").build());
            gallery.add(GalleryDTO.builder().title("Awards").icon("🥇").bgColor("#7c3aed").build());
            gallery.add(GalleryDTO.builder().title("Team Spirit").icon("🙌").bgColor("#16a34a").build());
        }

        String desc = customMessageOverride != null && !customMessageOverride.isBlank()
                ? customMessageOverride
                : (tournament.getDescription() != null ? tournament.getDescription() : "Join us for an exciting community tournament!");

        return TournamentAnnouncementEmailDTO.builder()
                .appName(appName)
                .tournamentName(tournament.getName())
                .tournamentDescription(desc)
                .organizerName(tournament.getContactName() != null ? tournament.getContactName() : "Sports Committee")
                .venueName(venueName)
                .venueAddress("Block C, Main Grounds")
                .eventStartDate(!evtStartStr.isBlank() ? evtStartStr : "Upcoming")
                .eventEndDate(!evtEndStr.isBlank() ? evtEndStr : "TBA")
                .registrationStartDate(!regStartStr.isBlank() ? regStartStr : "Open")
                .registrationEndDate(!regEndStr.isBlank() ? regEndStr : "Closing Soon")
                .announcementDate(!createdStr.isBlank() ? createdStr : "Published")
                .openingCeremonyDate(!evtStartStr.isBlank() ? evtStartStr : "Day 1")
                .fixturesDate(!regEndStr.isBlank() ? regEndStr : "TBA")
                .finalsDate(!evtEndStr.isBlank() ? evtEndStr : "Final Day")
                .expectedParticipants(tournament.getMaxParticipants() != null ? tournament.getMaxParticipants() : 500)
                .totalSports(sportsIncluded.isEmpty() ? 8 : sportsIncluded.size())
                .totalEvents(sportsEvents.isEmpty() ? 5 : sportsEvents.size())
                .customMessage(customMessageOverride)
                .actionButtonText("Register Now →")
                .actionUrl(support.props().getBaseUrl() + "/sports")
                .supportEmail(tournament.getContactEmail() != null ? tournament.getContactEmail() : "sports@manacommunity.app")
                .supportPhone(tournament.getContactNumber() != null ? tournament.getContactNumber() : "")
                .footerText("Bringing Communities Together Through Sports 🏆")
                .bannerImage(tournament.getBannerImage() != null ? tournament.getBannerImage() : "")
                .year(LocalDate.now().getYear())
                .sportsEvents(sportsEvents)
                .sportsIncluded(sportsIncluded)
                .timeline(timeline)
                .announcements(announcements)
                .galleryImages(gallery)
                .build();
    }

    /** Accent colour for a sport's icon tile, derived from the sport name. */
    private String sportColor(String sportName) {
        String s = sportName == null ? "" : sportName.toLowerCase();
        if (s.contains("cricket")) return "#16a34a";
        if (s.contains("badminton")) return "#7c3aed";
        if (s.contains("chess")) return "#334155";
        if (s.contains("football") || s.contains("soccer")) return "#0ea5e9";
        if (s.contains("tennis")) return "#f59e0b";
        if (s.contains("volleyball")) return "#ef4444";
        return "#2563eb";
    }

    public void sendTournamentRegistrationOpen(Tournament tournament) {
        sendTournamentNotification(tournament, EmailTemplate.REGISTRATION_OPEN, null, null);
    }

    public void sendTournamentAnnouncement(Tournament tournament, String customSubject, String customMessage) {
        sendTournamentNotification(tournament, EmailTemplate.TOURNAMENT_ANNOUNCEMENT, customSubject, customMessage);
    }

    private void sendTournamentNotification(Tournament tournament, EmailTemplate template, String customSubject, String customMessage) {
        if (tournament == null) return;

        List<AppUser> recipients;
        if (tournament.getCommunity() != null && tournament.getCommunity().getId() != null) {
            recipients = appUserRepository.findByCommunityIdAndIsActiveTrue(tournament.getCommunity().getId());
        } else {
            recipients = appUserRepository.findAll().stream()
                    .filter(u -> u != null && Boolean.TRUE.equals(u.getIsActive()))
                    .collect(Collectors.toList());
        }

        if (recipients == null || recipients.isEmpty()) {
            log.info("No active recipients found to notify about tournament '{}'", tournament.getName());
            return;
        }

        TournamentAnnouncementEmailDTO emailDTO = buildTournamentAnnouncementDTO(tournament, customMessage);

        List<EmailMessage> batch = new ArrayList<>();
        for (AppUser user : recipients) {
            if (user == null || support.isBlank(user.getEmail())) continue;
            try {
                // Single DTO drives the whole template — no per-field attributes.
                Map<String, Object> vars = support.baseVars(user.getFullName());
                vars.put("email", emailDTO);

                String subject = (customSubject != null && !customSubject.isBlank())
                        ? customSubject
                        : (template.defaultSubject() + " — " + tournament.getName());

                String html = renderer.render(template, vars);
                batch.add(new EmailMessage(user.getEmail(), user.getFullName(), subject, html));
            } catch (Exception e) {
                log.error("Failed to build tournament email for user {}", user.getId(), e);
            }
        }

        emailService.sendAll(batch);
        log.info("Queued {} tournament emails for '{}' using template {}", batch.size(), tournament.getName(), template.name());
    }
}




