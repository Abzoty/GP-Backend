package com.gp.GP_backend.domain.user.repository;

import com.gp.GP_backend.domain.user.entity.RefreshToken;
import com.gp.GP_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Data-access layer for {@link RefreshToken}.
 *
 * <p>
 * The custom {@code @Modifying} queries perform bulk UPDATE operations directly
 * in the database rather than loading and updating entities individually, which
 * is
 * significantly more efficient for revoking multiple tokens at once.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Look up a token by its opaque string value.
     * Used during token rotation and logout.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Returns ALL active (non-revoked, non-expired) tokens for a user.
     *
     * <p>
     * A user can have multiple active tokens if they are logged in from several
     * devices.
     * This replaces the previous {@code findByUser(User)} which returned only one
     * token
     * (incorrectly assuming a single session per user).
     */
    List<RefreshToken> findByUserAndRevokedFalse(User user);

    /**
     * Nightly cleanup: delete tokens that have expired OR have been revoked.
     * Invoked by {@link com.gp.GP_backend.shared.scheduled.TokenCleanupJob}.
     */
    void deleteByExpiryDateBeforeOrRevokedTrue(Instant now);

    /**
     * Revokes every token in a token family (reuse-attack mitigation).
     * Called when a previously rotated (already revoked) token is presented again —
     * this logs out all sessions that share the same login chain.
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.familyId = :familyId")
    void revokeAllByFamilyId(@Param("familyId") String familyId);

    /**
     * Logs a user out from all devices by revoking every token tied to their
     * account.
     * Used by the /logout-all endpoint.
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = :user")
    void revokeAllByUser(@Param("user") User user);
}