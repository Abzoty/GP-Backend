package com.gp.GP_backend.shared.scheduled;

import com.gp.GP_backend.domain.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled job that removes stale refresh tokens from the database.
 *
 * <p>
 * Without cleanup the {@code refresh_tokens} table would grow unboundedly
 * because:
 * <ul>
 * <li>Every login inserts a new row.</li>
 * <li>Every rotation inserts a new row (and marks the old one revoked).</li>
 * </ul>
 *
 * <p>
 * This job runs at 03:00 AM server time every day (low-traffic window) and
 * deletes
 * tokens that are either expired or revoked — neither of which can be used
 * anymore.
 *
 * <p>
 * Requires {@code @EnableScheduling} on
 * {@link com.gp.GP_backend.GpBackendApplication}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Deletes all refresh tokens that are expired or revoked.
     *
     * <p>
     * Cron expression {@code "0 0 3 * * *"} = second=0, minute=0, hour=3, every
     * day.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Running nightly refresh-token cleanup...");
        refreshTokenRepository.deleteByExpiryDateBeforeOrRevokedTrue(Instant.now());
        log.info("Refresh-token cleanup complete.");
    }
}