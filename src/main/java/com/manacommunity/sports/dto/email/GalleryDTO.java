package com.manacommunity.sports.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryDTO {
    private String imageUrl;
    private String caption;
    private String title;
    private String icon;
    private String bgColor;
}
