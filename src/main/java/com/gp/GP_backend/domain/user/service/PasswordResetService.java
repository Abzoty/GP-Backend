package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.user.entity.PasswordResetToken;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.PasswordResetTokenRepository;
import com.gp.GP_backend.domain.user.repository.UserRepository;
import com.gp.GP_backend.shared.exception.ApiException;
import com.gp.GP_backend.shared.util.EmailService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Handles both the forgot-password and change-password flows.
 *
 * <p>
 * <b>Forgot password (unauthenticated):</b>
 * <ol>
 * <li>Generate a cryptographically random raw token.</li>
 * <li>Store only its SHA-256 hash — the raw token goes exclusively over
 * email.</li>
 * <li>On redemption, hash the incoming token and compare against the stored
 * hash.</li>
 * <li>Revoke all refresh tokens so every existing session is invalidated.</li>
 * </ol>
 *
 * <p>
 * <b>Change password (authenticated):</b>
 * Verify the current password before accepting the new one, preventing an
 * attacker
 * with a stolen JWT from locking the real user out of their account.
 *
 * <p>
 * <b>Why {@code saveAndFlush} instead of {@code save}:</b>
 * {@link RefreshTokenRepository#revokeAllByUser} uses
 * {@code @Modifying(clearAutomatically = true)}, which wipes the JPA
 * persistence
 * context after the bulk UPDATE executes. Any changes that were only pending in
 * the
 * context (not yet flushed to the DB) are silently discarded at that point.
 * Calling {@code saveAndFlush} writes each change to the DB immediately, so the
 * subsequent context clear has nothing to discard.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long MIN_RESET_EXPIRY_MS = 60_000L; // 1 minute
    private static final long MAX_RESET_EXPIRY_MS = 86_400_000L; // 24 hours

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${reset.token-expiration-ms}")
    private long resetTokenExpiryMs;

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    @PostConstruct
    void validateResetExpiration() {
        if (resetTokenExpiryMs < MIN_RESET_EXPIRY_MS || resetTokenExpiryMs > MAX_RESET_EXPIRY_MS) {
            throw new IllegalStateException(
                    "reset.token-expiration-ms must be between "
                            + MIN_RESET_EXPIRY_MS
                            + " and "
                            + MAX_RESET_EXPIRY_MS
                            + " milliseconds");
        }
    }

    // ─── Forgot password ──────────────────────────────────────────────────────

    /**
     * Initiates the forgot-password flow for the given email address.
     *
     * <p>
     * <b>Always succeeds silently</b> — whether or not the email is registered,
     * the caller receives the same response. This prevents user-enumeration attacks
     * (OWASP A07).
     */
    @Transactional
    public void initiateForgotPassword(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        // Exit silently if the email is not registered — do not reveal this to the
        // caller
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();

        // Invalidate any existing unused reset tokens before issuing a new one
        passwordResetTokenRepository.invalidateAllForUser(user);

        String rawToken = generateSecureToken();
        String tokenHash = sha256Hex(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
            .expiryDate(Instant.now().plusMillis(resetTokenExpiryMs))
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Send the raw token — the backend never stores it in plain text
        String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetLink);
    }

    /**
     * Validates the reset token and applies the new password.
     *
     * <p>
     * Both writes (new password hash + token consumed flag) are flushed to the DB
     * with {@code saveAndFlush} <em>before</em> {@code revokeAllUserTokens} is
     * called.
     * This is necessary because {@code revokeAllByUser} carries
     * {@code clearAutomatically = true}, which wipes the persistence context after
     * the bulk UPDATE — any unflushed changes would otherwise be silently lost.
     *
     * @throws ApiException 400 if the token is invalid, expired, or already used.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = sha256Hex(rawToken);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token"));

        if (resetToken.isUsed()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reset token has expired. Please request a new one");
        }

        // Flush the new password to the DB immediately — must happen before
        // revokeAllUserTokens, which clears the persistence context (see class Javadoc)
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.saveAndFlush(user);

        // Mark the token as consumed and flush — same reason as above
        resetToken.setUsed(true);
        passwordResetTokenRepository.saveAndFlush(resetToken);

        // Revoke every active session — forces re-login on all devices.
        // Safe to call now that both writes are already committed to the DB.
        refreshTokenService.revokeAllUserTokens(user);
    }

    // ─── Change password (authenticated) ─────────────────────────────────────

    /**
     * Changes the password for an already-authenticated user.
     *
     * <p>
     * Requires the current password to be verified. This ensures a stolen JWT
     * alone is not sufficient to lock the legitimate user out of their account.
     *
     * <p>
     * Uses {@code saveAndFlush} for the same reason as {@link #resetPassword} —
     * the subsequent {@code revokeAllUserTokens} call would otherwise clear the
     * pending password change from the persistence context before it is written.
     *
     * @throws ApiException 400 if the current password is incorrect.
     */
    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        // Flush immediately — must precede revokeAllUserTokens (see class Javadoc)
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.saveAndFlush(user);

        // Invalidate any outstanding reset links after a successful password change.
        passwordResetTokenRepository.invalidateAllForUser(user);

        // Revoke all refresh tokens so other devices are prompted to re-authenticate
        refreshTokenService.revokeAllUserTokens(user);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Generates a 32-byte (256-bit) cryptographically secure random token,
     * returned as a 64-character hex string.
     */
    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * Returns the SHA-256 hex digest of the given string.
     * Used to store and compare tokens without persisting the raw value.
     */
    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JVM spec — this branch is unreachable
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}