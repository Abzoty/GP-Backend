package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.PasswordResetTokenRepository;
import com.gp.GP_backend.domain.user.repository.UserRepository;
import com.gp.GP_backend.shared.exception.ApiException;
import com.gp.GP_backend.shared.util.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

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

    @Test
    void changePasswordShouldInvalidateResetTokensAndRevokeSessions() {
        User user = User.builder()
                .email("user@test.com")
                .passwordHash("old-hash")
                .build();

        when(passwordEncoder.matches("old-pass", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPass1!")).thenReturn("new-hash");

        passwordResetService.changePassword(user, "old-pass", "NewPass1!");

        verify(userRepository).saveAndFlush(user);
        verify(passwordResetTokenRepository).invalidateAllForUser(user);
        verify(refreshTokenService).revokeAllUserTokens(user);
        assertEquals("new-hash", user.getPasswordHash());
    }

    @Test
    void changePasswordShouldRejectInvalidCurrentPassword() {
        User user = User.builder()
                .passwordHash("stored-hash")
                .build();

        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> passwordResetService.changePassword(user, "wrong", "NewPass1!"));

        assertEquals("Current password is incorrect", ex.getMessage());
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordResetTokenRepository, never()).invalidateAllForUser(any(User.class));
        verify(refreshTokenService, never()).revokeAllUserTokens(any(User.class));
    }
}
