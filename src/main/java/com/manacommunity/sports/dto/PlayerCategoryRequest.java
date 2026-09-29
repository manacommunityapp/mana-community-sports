package com.manacommunity.sports.dto;

import com.manacommunity.common.enums.*;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class PlayerCategoryRequest {
    @NotBlank
    private String name;
    
    @NotBlank
    private String categoryType;
    
    private String description;
    
    @NotNull
    private Integer minAge;
    
    @NotNull
    private Integer maxAge;
    
    @NotBlank
    private String gender;

    private String type;
    
    private Long communityId;
}

