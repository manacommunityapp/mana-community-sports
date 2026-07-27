package com.manacommunity.sports.model.scheduler;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity @Table(name = "tournament_group")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TournamentGroup {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id")
    private TournamentConfig config;

    @Column(nullable = false)
    private String groupName;     // "Group A", "Group B", ...

    @Column(nullable = false)
    private Integer groupOrder;   // 1, 2, 3 …

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<GroupTeamStanding> standings;
}
