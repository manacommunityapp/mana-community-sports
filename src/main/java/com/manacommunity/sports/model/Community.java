package com.manacommunity.sports.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * BUG FIX: Community entity had wrong table name ("communities" vs
 * "community"),
 * String PK instead of BIGSERIAL (Long), missing city/state/type fields,
 * and a spurious inviteCode field not present in the schema.
 *
 * NOTE: inviteCode is kept because CommunityRepository.findByInviteCode() and
 * AuthServiceImpl depend on it (app-level join logic, not a raw DB column).
 * If you want it in DB, add a column; otherwise keep only for in-memory use.
 * Here we keep it as a nullable column to avoid breaking existing logic.
 */
@Entity
@Table(name = "community")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String type; // APARTMENT, COLLEGE, SCHOOL, OFFICE

    @Column(length = 60)
    private String city;

    @Column(length = 40)
    private String state;

    @Column(length = 100)
    private String area;

    @Column(length = 50)
    private String subtype;
    /**
     * Application-level invite code (not in original schema; added for auth flow).
     */
    @Column(name = "invite_code", unique = true, length = 20)
    private String inviteCode;

    /**
     * Soft-delete flag. "Deleting" a community sets this to false instead of
     * removing the row, so historical references (users, events) stay intact.
     */
    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
    }
}
