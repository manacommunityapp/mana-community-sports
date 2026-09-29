package com.manacommunity.sports.controller;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Role;
import com.manacommunity.sports.dto.PlayerCategoryRequest;
import com.manacommunity.sports.model.PlayerCategory;
import com.manacommunity.sports.service.PlayerCategoryService;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.user.security.UserPrincipal;
import com.manacommunity.sports.user.service.LoggedInUserService;
import com.manacommunity.sports.service.PermissionCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

import static com.manacommunity.sports.constants.PermissionConstants.*;

@RestController
@RequestMapping("/api/player-categories")
@RequiredArgsConstructor
public class PlayerCategoryController {

    private final PlayerCategoryService categoryService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping
    public ResponseEntity<List<PlayerCategory>> getCategories(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(categoryService.getCategories(loggedInUser));
    }

    @PostMapping
    public ResponseEntity<PlayerCategory> createCategory(
            @Valid @RequestBody PlayerCategoryRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);

        AppUser loggedInUser = loggedInUserService.resolve(principal);
        String userRole = loggedInUser.getRole();
        String typeValue;
        switch (userRole) {
            case ROLE_SUPER_ADMIN:
                typeValue = "DEFAULT";
                break;
            case ROLE_ADMIN, ROLE_SPORTS_ADMIN:
                typeValue = "USER";
                break;
            case ROLE_VENDOR:
                typeValue = "VENDOR";
                break;
            default:
                throw new com.manacommunity.sports.exception.InvalidInputException("Unknown user role: " + userRole);
        }
        req.setType(typeValue);

        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlayerCategory> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody PlayerCategoryRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        return ResponseEntity.ok(categoryService.updateCategory(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, DELETE_SPORTS_MAIN);
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}



