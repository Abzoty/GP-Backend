package com.gp.GP_backend.domain.user.repository;

import com.gp.GP_backend.domain.user.entity.RefreshToken;
import com.gp.GP_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;


public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUser(User user);

    void deleteByExpiryDateBeforeOrRevokedTrue(Instant now);

    // revoke all tokens in a family (reuse attack detected)
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.familyId = :familyId")
    void revokeAllByFamilyId(@Param("familyId") String familyId);

    // logout from all devices
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = :user")
    void revokeAllByUser(@Param("user") User user);
}