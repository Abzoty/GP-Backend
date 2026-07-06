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


@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long MIN_RESET_EXPIRY_MS = 60_000L; // 1 minute
    private static final long MAX_RESET_EXPIRY_MS = 86_400_000L; // 24 hours

    @Value("${app.frontend-url:http://localhost:5173/reset-password}")
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

    @Transactional
    public void initiateForgotPassword(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

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

        String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetLink);
    }

    
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

        // Flush the new password to the DB
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.saveAndFlush(user);

        // Mark the token as consumed and flush
        resetToken.setUsed(true);
        passwordResetTokenRepository.saveAndFlush(resetToken);

        // Revoke every active session — forces re-login on all devices.
        refreshTokenService.revokeAllUserTokens(user);
    }

    // ─── Change password (authenticated) ─────────────────────────────────────

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        // Flush immediately 
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.saveAndFlush(user);

        // Invalidate any outstanding reset links after a successful password change.
        passwordResetTokenRepository.invalidateAllForUser(user);

        // Revoke all refresh tokens so other devices are prompted to re-authenticate
        refreshTokenService.revokeAllUserTokens(user);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}