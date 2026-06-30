package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.user.entity.PasswordResetToken;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.PasswordResetTokenRepository;
import com.gp.GP_backend.domain.user.repository.UserRepository;
import com.gp.GP_backend.shared.exception.ApiException;
import com.gp.GP_backend.shared.util.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Extends {@link PasswordResetServiceTest}, which only covers
 * {@code changePassword}. This class verifies the forgot-password /
 * reset-password token flow.
 */
@ExtendWith(MockitoExtension.class)
public class PasswordResetServiceExtendedTest {

    private static final long RESET_TOKEN_EXPIRY_MS = 15 * 60 * 1000L; // 15 minutes

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        // @Value fields are not populated by MockitoExtension; set them directly.
        ReflectionTestUtils.setField(passwordResetService, "resetTokenExpiryMs", RESET_TOKEN_EXPIRY_MS);
        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "http://localhost:5173/reset-password");
    }

    // ─── initiateForgotPassword ────────────────────────────────────────────

    @Test
    void initiateForgotPasswordShouldSucceedSilentlyForUnknownEmail() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        // Must not throw, and must not touch the token repo or email service.
        passwordResetService.initiateForgotPassword("ghost@test.com");

        verify(passwordResetTokenRepository, never()).invalidateAllForUser(any(User.class));
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void initiateForgotPasswordShouldInvalidateOldTokensBeforeIssuingNewOne() {
        User user = User.builder().email("user@test.com").fullName("User Test").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        passwordResetService.initiateForgotPassword("user@test.com");

        verify(passwordResetTokenRepository).invalidateAllForUser(user);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void initiateForgotPasswordShouldStoreOnlyTheTokenHashAndEmailOnlyTheRawToken() {
        User user = User.builder().email("user@test.com").fullName("User Test").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        passwordResetService.initiateForgotPassword("user@test.com");

        org.mockito.ArgumentCaptor<PasswordResetToken> tokenCaptor = org.mockito.ArgumentCaptor
                .forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        // The persisted token must be a 64-char hex SHA-256 digest, never a raw value.
        assertEquals(64, savedToken.getTokenHash().length());
        assertTrue(savedToken.getTokenHash().matches("^[0-9a-f]{64}$"));

        org.mockito.ArgumentCaptor<String> linkCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("user@test.com"), eq("User Test"), linkCaptor.capture());

        // The raw token that goes out over email must be present in the link...
        String resetLink = linkCaptor.getValue();
        String rawToken = resetLink.substring(resetLink.indexOf("token=") + "token=".length());
        assertEquals(64, rawToken.length());
        // ...and must NOT equal the stored hash — the DB only ever holds the digest.
        assertTrue(!rawToken.equals(savedToken.getTokenHash()),
                "raw token must differ from its own SHA-256 hash");
    }

    // ─── resetPassword ──────────────────────────────────────────────────────

    @Test
    void resetPasswordWithValidTokenShouldChangePasswordAndMarkTokenUsed() {
        User user = User.builder().email("user@test.com").passwordHash("old-hash").build();
        PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(sha256Hex("raw-token"))
                .user(user)
                .expiryDate(Instant.now().plusSeconds(60))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByTokenHash(sha256Hex("raw-token")))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPass1!")).thenReturn("new-hash");

        passwordResetService.resetPassword("raw-token", "NewPass1!");

        assertEquals("new-hash", user.getPasswordHash());
        assertTrue(token.isUsed());
        verify(userRepository).saveAndFlush(user);
        verify(passwordResetTokenRepository).saveAndFlush(token);
    }

    @Test
    void resetPasswordWithValidTokenShouldRevokeAllRefreshTokens() {
        User user = User.builder().email("user@test.com").passwordHash("old-hash").build();
        PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(sha256Hex("raw-token"))
                .user(user)
                .expiryDate(Instant.now().plusSeconds(60))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByTokenHash(sha256Hex("raw-token")))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode(anyString())).thenReturn("new-hash");

        passwordResetService.resetPassword("raw-token", "NewPass1!");

        verify(refreshTokenService, times(1)).revokeAllUserTokens(user);
    }

    @Test
    void resetPasswordWithAlreadyUsedTokenShouldReturn400() {
        User user = User.builder().email("user@test.com").build();
        PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(sha256Hex("raw-token"))
                .user(user)
                .expiryDate(Instant.now().plusSeconds(60))
                .used(true)
                .build();

        when(passwordResetTokenRepository.findByTokenHash(sha256Hex("raw-token")))
                .thenReturn(Optional.of(token));

        ApiException ex = assertThrows(ApiException.class,
                () -> passwordResetService.resetPassword("raw-token", "NewPass1!"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(refreshTokenService, never()).revokeAllUserTokens(any(User.class));
    }

    @Test
    void resetPasswordWithExpiredTokenShouldReturn400() {
        User user = User.builder().email("user@test.com").build();
        PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(sha256Hex("raw-token"))
                .user(user)
                .expiryDate(Instant.now().minusSeconds(60))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByTokenHash(sha256Hex("raw-token")))
                .thenReturn(Optional.of(token));

        ApiException ex = assertThrows(ApiException.class,
                () -> passwordResetService.resetPassword("raw-token", "NewPass1!"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(refreshTokenService, never()).revokeAllUserTokens(any(User.class));
    }

    @Test
    void resetPasswordWithNonExistentTokenShouldReturn400() {
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> passwordResetService.resetPassword("unknown-token", "NewPass1!"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    // ─── validateResetExpiration (startup guard) ───────────────────────────

    @Test
    void validateResetExpirationShouldRejectValuesBelowMinimum() {
        ReflectionTestUtils.setField(passwordResetService, "resetTokenExpiryMs", 1_000L); // < 1 minute

        assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(passwordResetService, "validateResetExpiration"));
    }

    @Test
    void validateResetExpirationShouldRejectValuesAboveMaximum() {
        ReflectionTestUtils.setField(passwordResetService, "resetTokenExpiryMs", 100_000_000L); // > 24 hours

        assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(passwordResetService, "validateResetExpiration"));
    }

    @Test
    void validateResetExpirationShouldAcceptValuesWithinRange() {
        ReflectionTestUtils.setField(passwordResetService, "resetTokenExpiryMs", RESET_TOKEN_EXPIRY_MS);

        ReflectionTestUtils.invokeMethod(passwordResetService, "validateResetExpiration");
        // No exception thrown — value is within the [1 min, 24 h] range.
    }

    // ─── helper ─────────────────────────────────────────────────────────────

    private String sha256Hex(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}