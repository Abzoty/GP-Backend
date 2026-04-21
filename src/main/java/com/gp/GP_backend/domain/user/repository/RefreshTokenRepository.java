package com.gp.GP_backend.domain.user.repository;

import com.gp.GP_backend.domain.user.entity.RefreshToken;
import com.gp.GP_backend.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for {@link RefreshToken} management.
 *
 * <p>
 * Custom JPQL {@code @Modifying} queries are used for bulk revocation
 * because Spring Data's derived delete methods issue one DELETE per entity
 * (N queries), while JPQL UPDATE issues a single query.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** Primary lookup used during token rotation and logout. */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Locked lookup used in refresh-token rotation/revocation flows
     * to avoid concurrent updates of the same token row.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefreshToken r WHERE r.token = :token")
    Optional<RefreshToken> findByTokenForUpdate(@Param("token") String token);

    /**
     * Revokes all tokens that share a family (same login session).
     * Called when token reuse is detected to neutralise a potential theft.
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.familyId = :familyId")
    void revokeAllByFamilyId(@Param("familyId") String familyId);

    /**
     * Revokes all tokens for a user regardless of family.
     * Called on logout-all-devices and on new login (to clean old sessions).
     * {@code clearAutomatically = true} clears the persistence context after
     * the bulk UPDATE so that subsequent reads reflect the new state.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = :user")
    void revokeAllByUser(@Param("user") User user);

    /**
     * Deletes tokens that are either expired or already revoked.
     * Called nightly by {@link com.gp.GP_backend.shared.scheduled.TokenCleanupJob}.
     */
    void deleteByExpiryDateBeforeOrRevokedTrue(Instant cutoff);
}
