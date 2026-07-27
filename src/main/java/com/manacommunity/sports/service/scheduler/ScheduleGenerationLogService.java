package com.manacommunity.sports.service.scheduler;

import com.manacommunity.sports.dto.scheduler.ScheduleGenerationLogResponse;
import com.manacommunity.sports.user.model.AppUser;
import com.manacommunity.sports.model.scheduler.GenerationAction;
import com.manacommunity.sports.model.scheduler.GenerationStatus;
import com.manacommunity.sports.model.scheduler.ScheduleGenerationLog;
import com.manacommunity.sports.user.repository.AppUserRepository;
import com.manacommunity.sports.repository.scheduler.ScheduleGenerationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages the immutable schedule_generation_log audit trail.
 * All writes use REQUIRES_NEW propagation so that log entries are persisted
 * even if the calling transaction rolls back (e.g., logging a FAILED attempt).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleGenerationLogService {

    private final ScheduleGenerationLogRepository logRepo;
    private final AppUserRepository               userRepo;

    // ── Write ────────────────────────────────────────────────────────

    /**
     * Records a generation/save operation in the audit log.
     * Uses REQUIRES_NEW so log entries survive caller rollbacks.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScheduleGenerationLog log(
            GenerationAction action,
            GenerationStatus status,
            Long configId,
            Long eventId,
            Long communityId,
            Long userId,
            String tournamentType,
            Integer totalTeams,
            Integer totalMatches,
            Integer totalGroups,
            String venuesUsed,
            Long durationMs,
            String errorMessage,
            String requestSnapshot,
            String responseSnapshot,
            String ipAddress
    ) {
        ScheduleGenerationLog entry = ScheduleGenerationLog.builder()
                .configId(configId)
                .eventId(eventId)
                .communityId(communityId)
                .generatedBy(userId)
                .action(action)
                .status(status)
                .tournamentType(tournamentType)
                .totalTeams(totalTeams)
                .totalMatches(totalMatches)
                .totalGroups(totalGroups)
                .venuesUsed(venuesUsed)
                .durationMs(durationMs)
                .errorMessage(errorMessage)
                .requestSnapshot(requestSnapshot)
                .responseSnapshot(responseSnapshot)
                .ipAddress(ipAddress)
                .build();
        entry = logRepo.save(entry);
        log.debug("Schedule generation log [{}]: action={} status={} configId={}",
                entry.getId(), action, status, configId);
        return entry;
    }

    // ── Read ─────────────────────────────────────────────────────────

    public List<ScheduleGenerationLogResponse> getLogsByConfig(Long configId) {
        return logRepo.findByConfigIdOrderByCreatedAtDesc(configId)
                .stream().map(this::toResponse).toList();
    }

    public List<ScheduleGenerationLogResponse> getLogsByEvent(Long eventId) {
        return logRepo.findByEventIdOrderByCreatedAtDesc(eventId)
                .stream().map(this::toResponse).toList();
    }

    public Page<ScheduleGenerationLogResponse> getLogsByCommunity(Long communityId, int page, int size) {
        return logRepo.findByCommunityIdOrderByCreatedAtDesc(communityId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    // ── Mapping ──────────────────────────────────────────────────────

    private ScheduleGenerationLogResponse toResponse(ScheduleGenerationLog e) {
        String generatedByName = null;
        if (e.getGeneratedBy() != null) {
            generatedByName = userRepo.findById(e.getGeneratedBy())
                    .map(AppUser::getFullName)
                    .orElse(null);
        }
        return new ScheduleGenerationLogResponse(
                e.getId(), e.getConfigId(), e.getEventId(), e.getCommunityId(),
                e.getGeneratedBy(), generatedByName,
                e.getAction() != null ? e.getAction().name() : null,
                e.getStatus() != null ? e.getStatus().name() : null,
                e.getTournamentType(),
                e.getTotalTeams(), e.getTotalMatches(), e.getTotalGroups(),
                e.getDurationMs(), e.getErrorMessage(), e.getCreatedAt()
        );
    }
}
