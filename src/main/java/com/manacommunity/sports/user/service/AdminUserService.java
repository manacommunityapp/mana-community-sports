package com.manacommunity.sports.user.service;

import com.manacommunity.sports.user.dto.AdminCreateUserRequest;
import com.manacommunity.sports.user.model.AppUser;

/** Admin-facing user management (create-user page and related operations). */
public interface AdminUserService {

    /**
     * Creates a fully-formed community user from the admin create-user form:
     * resolves the target community and role (with its permissions), hashes the
     * password, and persists all captured profile fields. Returns the saved user.
     */
    AppUser createUser(AdminCreateUserRequest request);
}
