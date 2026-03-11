package com.gp.GP_backend.shared.scheduled;

import com.gp.GP_backend.domain.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Nightly scheduled job that purges stale refresh tokens from the database.
 *
 * <p>
 * Without this job, the {@code refresh_tokens} table would grow indefinitely
 * as users log in and log out over time. Tokens are safe to delete once:
 * <ul>
 * <li>Their {@code expiry_date} has passed (they can no longer be used),
 * OR</li>
 * <li>They have been explicitly revoked (logout / reuse detection).</li>
 * </ul>
 *
 * <p>
 * The job runs at 3 AM daily (server time) when load is minimal.
 * The cron expression {@code "0 0 3 * * *"} means: second=0, minute=0, hour=3,
 * any day-of-month, any month, any day-of-week.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredAndRevokedTokens() {
        Instant now = Instant.now();
        log.info("Running nightly token cleanup at {}", now);
        refreshTokenRepository.deleteByExpiryDateBeforeOrRevokedTrue(now);
        log.info("Token cleanup complete");
    }
}
