package com.manacommunity.sports.user.dto;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO — set or update CRUD flags for a single role + menu item.
 */
public record MenuRolePermissionRequest(
    @NotNull Long roleId,
    @NotNull Long menuId,
    Boolean canView,
    Boolean canAdd,
    Boolean canUpdate,
    Boolean canDelete
) {}


