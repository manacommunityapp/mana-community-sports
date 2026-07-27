package com.manacommunity.sports.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportEventDTO {
    private String eventName;
    private String sportName;
    private String icon;
    private String gender;
    private String ageRange;
    private String eventDate;
    private String venueName;
    /** Presentation-only accent colour for the sport icon tile (resolved in the service). */
    private String iconBgColor;
}
