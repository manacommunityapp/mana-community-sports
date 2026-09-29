package com.manacommunity.sports.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineDTO {
    private String phase;
    private String dateRange;
    private String description;
    private boolean active;
    private String date;
    private String title;
}
