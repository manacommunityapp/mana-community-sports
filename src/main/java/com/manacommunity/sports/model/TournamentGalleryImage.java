package com.manacommunity.sports.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A gallery/memory tile attached to a {@link Tournament}, shown in the
 * "Memories &amp; Gallery" section of the announcement email.
 */
@Entity
@Table(name = "tournament_gallery_image")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentGalleryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @JsonIgnoreProperties({"sportsEvents", "sponsors", "contacts"})
    private Tournament tournament;

    @Column(length = 100)
    private String title;

    // TEXT: may hold a URL or an inline base64 data-URI.
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "bg_color", length = 20)
    private String bgColor;

    @Column(length = 20)
    private String icon;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
