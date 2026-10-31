package com.manacommunity.sports.support;

import com.manacommunity.common.model.Community;
import com.manacommunity.common.model.Role;
import com.manacommunity.common.model.RolePermission;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.sports.model.*;
import com.manacommunity.sports.model.scheduler.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.manacommunity.sports.model.scheduler.TournamentConfig.TournamentStatus;

/**
 * Factory class for building test entities.
 * Use these builders instead of constructing entities inline so that
 * required fields never get missed and changes to entity structure
 * only need to be fixed in one place.
 */
public final class TestDataBuilder {

    private TestDataBuilder() {}

    // ── Community ─────────────────────────────────────────────────────

    public static Community community(Long id, String inviteCode) {
        return Community.builder()
                .id(id)
                .name("Test Community")
                .inviteCode(inviteCode)
                .build();
    }

    public static Community community() {
        return community(1L, "INVITE123");
    }

    // ── Role ─────────────────────────────────────────────────────────

    public static Role role(Long id, String name, Long communityId) {
        return Role.builder()
                .id(id)
                .name(name)
                .communityId(communityId)
                .build();
    }

    public static Role memberRole() { return role(10L, "MEMBER", 1L); }
    public static Role adminRole()  { return role(11L, "ADMIN",  1L); }

    // ── AppUser ───────────────────────────────────────────────────────

    public static AppUser appUser(Long id, String email, String role) {
        return AppUser.builder()
                .id(id)
                .fullName("Test User")
                .email(email)
                .phone("9" + id + "0000001")
                .passwordHash("$2a$10$hashed")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("MALE")
                .role(role)
                .kycStatus("VERIFIED")
                .isActive(true)
                .community(community())
                .build();
    }

    public static AppUser adminUser()  { return appUser(1L, "admin@test.com", "ADMIN"); }
    public static AppUser memberUser() { return appUser(2L, "member@test.com", "MEMBER"); }
    public static AppUser superAdmin() { return appUser(3L, "super@test.com", "SUPER_ADMIN"); }

    // ── RolePermission ────────────────────────────────────────────────

    public static RolePermission rolePermission(Role role, String permissionKey) {
        return RolePermission.builder()
                .role(role.getName().toUpperCase())
                .roleEntity(role)
                .permissionKey(permissionKey)
                .build();
    }



    // ── TournamentConfig ──────────────────────────────────────────────

    public static TournamentConfig tournamentConfig(Long id) {
        return TournamentConfig.builder()
                .id(id)
                .tournamentName("Test Cup")
                .tournamentType(TournamentType.KNOCKOUT)
                .totalTeams(8)
                .startDate(LocalDate.now().plusDays(7))
                .status(TournamentStatus.DRAFT)
                .build();
    }

    // ── TournamentMatch ───────────────────────────────────────────────

    public static TournamentMatch match(TournamentConfig config, int number) {
        return TournamentMatch.builder()
                .config(config)
                .round(MatchRound.FINAL)
                .matchNumber(number)
                .scheduledAt(LocalDateTime.now().plusDays(7))
                .durationMinutes(90)
                .status(MatchStatus.SCHEDULED)
                .build();
    }
}
