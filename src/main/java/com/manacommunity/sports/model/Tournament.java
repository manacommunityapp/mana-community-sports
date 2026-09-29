package com.manacommunity.sports.model;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity 
@Table(name = "tournament")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class Tournament {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(length = 2000)
    private String description;

    // One Tournament contains Many Sports Events.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Community community;

    // One Tournament has Many Sports Events (1:N). Inverse side — the owning
    // side is SportsEvent.tournament, whose @JoinColumn maps the FK
    // sports_event.tournament_id. @Builder.Default is required because Lombok
    // @Builder ignores the field initializer without it, which would otherwise
    // leave sportsEvents null on builder-created Tournament rows.
    //
    // Cascade is PERSIST+MERGE only (NOT ALL/REMOVE): deleting a tournament must
    // NOT delete its events (they own registrations, matches, etc., and would
    // block the delete). deleteTournament() disassociates the events instead.
    @OneToMany(mappedBy = "tournament", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnoreProperties({"tournament"})
    @Builder.Default
    private List<SportsEvent> sportsEvents = new ArrayList<>();

    private LocalDate eventDateStart;
    private LocalDate eventDateEnd;
    private LocalDate registrationDateStart;
    private LocalDate registrationDateEnd;

    private String startTime;
    private String dueTime;

    // Stores either a URL or an inline base64 data-URI, so it must not be
    // capped at the default varchar(255) — base64 images overflow it and
    // Postgres raises SQLSTATE 22001 ("value too long"). Map to TEXT.
    @Column(columnDefinition = "TEXT")
    private String bannerImage;

    private String contactName;
    private String contactNumber;
    private String contactEmail;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "tournament_contact",
        joinColumns = @JoinColumn(name = "tournament_id"),
        inverseJoinColumns = @JoinColumn(name = "contact_id")
    )
    @Builder.Default
    private List<Contact> contacts = new ArrayList<>();

    @Column(length = 5000)
    private String otherContacts;

    private Boolean allowAdminChat;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"tournament"})
    @Builder.Default
    private List<SportsEventSponsor> sponsors = new java.util.ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", length = 20)
    private EventStatus registrationStatus;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Link a SportsEvent to this tournament, keeping both sides of the 1:N in
     * sync (sets the FK on the event and adds it to this collection).
     */
    public void addSportsEvent(SportsEvent event) {
        if (event == null) return;
        sportsEvents.add(event);
        event.setTournament(this);
    }

    /** Detach a SportsEvent from this tournament, clearing its FK. */
    public void removeSportsEvent(SportsEvent event) {
        if (event == null) return;
        sportsEvents.remove(event);
        event.setTournament(null);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum EventStatus { DRAFT, REGISTRATION_OPEN, REGISTRATION_CLOSED, LIVE, COMPLETED, CANCELLED }
    public enum AuctionEventStatus { DRAFT, ACTIVE, LIVE, COMPLETED, CANCELLED }
    public enum MatchFormat { SINGLES, DOUBLES, MIXED_DOUBLES, TEAM }
    public enum TournamentType { KNOCKOUT, ROUND_ROBIN, LEAGUE, KNOCKOUT_SINGLE, KNOCKOUT_DOUBLE, GROUP_PLAYOFF, CUSTOM }
}


