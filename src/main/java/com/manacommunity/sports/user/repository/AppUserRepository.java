package com.manacommunity.sports.user.repository;

import com.manacommunity.sports.user.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * BUG FIX: Renamed from UserRepository to AppUserRepository to match
 * the AppUser entity (used in SportsEventServiceImpl, AuctionServiceImpl,
 * NotificationScheduler). The old UserRepository worked against the
 * legacy User entity (String PK / "users" table) which conflicts with
 * the schema's app_user table (Long PK).
 *
 * Changed PK type to Long matching AppUser.id (BIGSERIAL).
 * Added findByPhone for duplicate-phone check during registration.
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    java.util.List<AppUser> findByCommunityIdAndFullNameContainingIgnoreCase(Long communityId, String query);
    java.util.List<AppUser> findByCommunityId(Long communityId);
    Page<AppUser> findByCommunityId(Long communityId, Pageable pageable);
    java.util.List<AppUser> findByCommunityIdAndIsActiveTrue(Long communityId);
    Page<AppUser> findByKycStatus(String kycStatus, Pageable pageable);
    Page<AppUser> findByCommunityIdAndKycStatus(Long communityId, String kycStatus, Pageable pageable);
    long countByKycStatus(String kycStatus);
    long countByCommunityIdAndKycStatus(Long communityId, String kycStatus);
    long countByCommunityId(Long communityId);
}
