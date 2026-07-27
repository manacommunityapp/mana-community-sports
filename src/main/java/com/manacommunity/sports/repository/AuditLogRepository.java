package com.manacommunity.sports.repository;

import com.manacommunity.sports.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Paged search with optional module / action / userId filters (any may be null). */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:module IS NULL OR a.module = :module)
              AND (:action IS NULL OR a.action = :action)
              AND (:userId IS NULL OR a.userId = :userId)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> search(@Param("module") String module,
                          @Param("action") String action,
                          @Param("userId") Long userId,
                          Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime since);

    long countByActionAndCreatedAtAfter(String action, LocalDateTime since);

    long countByModuleAndCreatedAtAfter(String module, LocalDateTime since);
}
