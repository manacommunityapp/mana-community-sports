package com.manacommunity.sports.model;

import com.manacommunity.sports.user.model.AppUser;

import jakarta.persistence.*;
import lombok.*;

/**
 * RolePermission Entity
 * Maps user roles (e.g. ADMIN, MEMBER, VENDOR) to dynamic menu-based permission keys.
 */
@Entity
@Table(
    name = "role_permissions",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"role_id", "permission_key", "user_id"})}
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"user", "roleEntity"})
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(length = 50)
    private String role; // e.g. "ADMIN", "MEMBER", "VENDOR", "CASHIER", "STAFF"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Role roleEntity;

    @Column(name = "permission_key", nullable = false, length = 100)
    private String permissionKey; // e.g. "View Feed", "Create Post", "View Sports", etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private AppUser user;
}
