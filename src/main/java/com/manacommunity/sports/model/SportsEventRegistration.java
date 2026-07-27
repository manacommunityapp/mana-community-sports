package com.manacommunity.sports.model;

import com.manacommunity.sports.user.model.AppUser;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity @Table(name = "sports_event_registration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsEventRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private SportsEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private PlayerCategory category;

    @Enumerated(EnumType.STRING)
    private SportsEvent.MatchFormat matchType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_user_id")
    private AppUser partner;

    @Enumerated(EnumType.STRING)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    private String playerName;
    private String email;
    private String relation;
    private String flatNumber;
    private Integer age;
    private String role;

    @Column(name = "reject_reason", length = 1000)
    private String rejectReason;

    private Boolean captainNomination;
    private Boolean captainConfirmation;
    private String proposedTeamName;

    private LocalDateTime registeredAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum RegistrationStatus { PENDING, REGISTERED, CONFIRMED, WITHDRAWN, REJECTED }
}
