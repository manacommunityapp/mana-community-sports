package com.manacommunity.sports.repository.scheduler;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.scheduler.ScheduleGenerationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleGenerationLogRepository extends JpaRepository<ScheduleGenerationLog, Long> {

    List<ScheduleGenerationLog> findByConfigIdOrderByCreatedAtDesc(Long configId);

    List<ScheduleGenerationLog> findByEventIdOrderByCreatedAtDesc(Long eventId);

    Page<ScheduleGenerationLog> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);

    Page<ScheduleGenerationLog> findByGeneratedByOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

