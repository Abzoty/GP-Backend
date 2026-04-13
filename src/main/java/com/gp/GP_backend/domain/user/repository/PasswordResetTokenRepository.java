package com.gp.GP_backend.domain.user.repository;

import com.gp.GP_backend.domain.user.entity.PasswordResetToken;
import com.gp.GP_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for {@link PasswordResetToken} entities.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Primary lookup — tokens are always retrieved by their hash, never by raw
     * value.
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Invalidates any existing reset tokens for a user before issuing a new one.
     * Prevents a user from holding multiple valid reset links simultaneously.
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.user = :user AND t.used = false")
    void invalidateAllForUser(@Param("user") User user);

    /**
     * Removes tokens that are either expired or already used.
     * Called nightly by {@link com.gp.GP_backend.shared.scheduled.TokenCleanupJob}.
     */
    void deleteByExpiryDateBeforeOrUsedTrue(Instant cutoff);
}