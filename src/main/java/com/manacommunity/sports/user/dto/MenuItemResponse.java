package com.manacommunity.sports.user.dto;

import com.manacommunity.common.enums.*;
import java.util.List;

public record MenuItemResponse(
    Long id,
    String menuKey,
    String label,
    String icon,
    String routePath,
    Long parentId,
    Integer sortOrder,
    Boolean isActive,
    String permissionKey,
    List<MenuItemResponse> children
) {}

