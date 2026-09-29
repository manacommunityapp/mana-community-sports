package com.manacommunity.sports.dto;

import com.manacommunity.common.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactDto {
    private Long id;
    private String name;
    private String title;
    private String number;
    private String email;
}

