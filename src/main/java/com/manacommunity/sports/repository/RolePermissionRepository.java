package com.manacommunity.sports.repository;

import com.manacommunity.sports.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRoleIgnoreCase(String role);

    void deleteByRoleIgnoreCase(String role);

    void deleteByRoleIgnoreCaseAndUserId(String role, Long userId);

    List<RolePermission> findByRoleIn(List<String> roles);

    List<RolePermission> findByUserId(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RolePermission rp WHERE rp.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RolePermission rp WHERE rp.roleEntity.id = :roleId AND rp.user IS NULL")
    void deleteByRoleEntityIdAndUserIsNull(@Param("roleId") Long roleId);
}
