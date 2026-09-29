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
    private String sportName;
    private String eventName;
    private String category;
    private String format;
    private Integer maxParticipants;
    private String icon;
    private String gender;
    private String ageRange;
    private String eventDate;
    private String venueName;
    private String iconBgColor;
}
