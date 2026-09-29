package com.manacommunity.sports.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementDTO {
    private String title;
    private String date;
    private String message;
    private String priority;
    private String icon;
    private String content;
}
