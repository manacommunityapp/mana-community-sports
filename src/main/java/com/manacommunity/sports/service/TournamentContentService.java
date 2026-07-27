package com.manacommunity.sports.service;

import com.manacommunity.sports.exception.ResourceNotFoundException;
import com.manacommunity.sports.model.Tournament;
import com.manacommunity.sports.model.TournamentAnnouncement;
import com.manacommunity.sports.model.TournamentGalleryImage;
import com.manacommunity.sports.model.TournamentTimelineEntry;
import com.manacommunity.sports.repository.TournamentAnnouncementRepository;
import com.manacommunity.sports.repository.TournamentGalleryImageRepository;
import com.manacommunity.sports.repository.TournamentRepository;
import com.manacommunity.sports.repository.TournamentTimelineEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * CRUD for the custom content shown in a tournament's announcement email:
 * announcements, gallery images and timeline milestones.
 */
@Service
@RequiredArgsConstructor
public class TournamentContentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentAnnouncementRepository announcementRepository;
    private final TournamentGalleryImageRepository galleryRepository;
    private final TournamentTimelineEntryRepository timelineRepository;

    private Tournament requireTournament(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));
    }

    // ── Announcements ────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TournamentAnnouncement> listAnnouncements(Long tournamentId) {
        return announcementRepository.findByTournamentIdOrderBySortOrderAscIdAsc(tournamentId);
    }

    @Transactional
    public TournamentAnnouncement addAnnouncement(Long tournamentId, String title, String content,
                                                  String icon, Integer sortOrder) {
        Tournament tournament = requireTournament(tournamentId);
        return announcementRepository.save(TournamentAnnouncement.builder()
                .tournament(tournament)
                .title(title)
                .content(content)
                .icon(icon)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .build());
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        if (!announcementRepository.existsById(id)) throw new ResourceNotFoundException("TournamentAnnouncement", id);
        announcementRepository.deleteById(id);
    }

    // ── Gallery ──────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TournamentGalleryImage> listGallery(Long tournamentId) {
        return galleryRepository.findByTournamentIdOrderBySortOrderAscIdAsc(tournamentId);
    }

    @Transactional
    public TournamentGalleryImage addGalleryImage(Long tournamentId, String title, String imageUrl,
                                                  String bgColor, String icon, Integer sortOrder) {
        Tournament tournament = requireTournament(tournamentId);
        return galleryRepository.save(TournamentGalleryImage.builder()
                .tournament(tournament)
                .title(title)
                .imageUrl(imageUrl)
                .bgColor(bgColor)
                .icon(icon)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .build());
    }

    @Transactional
    public void deleteGalleryImage(Long id) {
        if (!galleryRepository.existsById(id)) throw new ResourceNotFoundException("TournamentGalleryImage", id);
        galleryRepository.deleteById(id);
    }

    // ── Timeline ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TournamentTimelineEntry> listTimeline(Long tournamentId) {
        return timelineRepository.findByTournamentIdOrderBySortOrderAscIdAsc(tournamentId);
    }

    @Transactional
    public TournamentTimelineEntry addTimelineEntry(Long tournamentId, LocalDate entryDate, String dateLabel,
                                                    String title, String description, Integer sortOrder) {
        Tournament tournament = requireTournament(tournamentId);
        return timelineRepository.save(TournamentTimelineEntry.builder()
                .tournament(tournament)
                .entryDate(entryDate)
                .dateLabel(dateLabel)
                .title(title)
                .description(description)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .build());
    }

    @Transactional
    public void deleteTimelineEntry(Long id) {
        if (!timelineRepository.existsById(id)) throw new ResourceNotFoundException("TournamentTimelineEntry", id);
        timelineRepository.deleteById(id);
    }
}
