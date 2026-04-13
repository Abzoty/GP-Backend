package com.gp.GP_backend.shared.scheduled;

import com.gp.GP_backend.domain.user.repository.PasswordResetTokenRepository;
import com.gp.GP_backend.domain.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Nightly scheduled job that purges stale tokens from the database.
 *
 * <p>
 * Two token tables are cleaned:
 * <ul>
 * <li>{@code refresh_tokens} — removes expired or revoked session tokens.</li>
 * <li>{@code password_reset_tokens} — removes expired or already-used reset
 * tokens.</li>
 * </ul>
 *
 * <p>
 * Runs at 3 AM daily (server time) when load is minimal.
 * The cron expression {@code "0 0 3 * * *"}: second=0, minute=0, hour=3,
 * any day-of-month, any month, any day-of-week.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupStaleTokens() {
        Instant now = Instant.now();
        log.info("Running nightly token cleanup at {}", now);

        refreshTokenRepository.deleteByExpiryDateBeforeOrRevokedTrue(now);
        log.info("Refresh token cleanup complete");

        passwordResetTokenRepository.deleteByExpiryDateBeforeOrUsedTrue(now);
        log.info("Password reset token cleanup complete");
    }
}