package com.manacommunity.sports.model.scheduler;

import com.manacommunity.sports.model.AuctionTeam;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "group_team_standing")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupTeamStanding {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private TournamentGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private AuctionTeam team;

    private Integer seedRank;    // seeding position within group

    // ── Live stats (updated after each match result) ──────────────
    @Builder.Default private Integer played   = 0;
    @Builder.Default private Integer won      = 0;
    @Builder.Default private Integer lost     = 0;
    @Builder.Default private Integer drawn    = 0;
    @Builder.Default private Integer points   = 0;
    private Integer runsFor;       // for NRR in cricket
    private Integer runsAgainst;
    private Integer oversFor;
    private Integer oversAgainst;
    @Builder.Default private Double  netRunRate = 0.0;

    /** Is this team qualified to next round? */
    @Builder.Default private Boolean qualified = false;
    @Builder.Default private Boolean eliminated = false;
}
