package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.user.entity.RefreshToken;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.RefreshTokenRepository;
import com.gp.GP_backend.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenFamilyRevoker tokenFamilyRevoker;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void createRefreshTokenShouldRevokePreviousAndCreateNewTokenFamily() {
        User user = user();
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 86_400_000L);

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken(user);

        assertEquals(user, token.getUser());
        assertNotNull(token.getToken());
        assertNotNull(token.getFamilyId());
        assertFalse(token.isRevoked());
        verify(refreshTokenRepository).revokeAllByUser(user);
        verify(refreshTokenRepository).flush();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void rotateRefreshTokenShouldThrowUnauthorizedWhenTokenMissing() {
        when(refreshTokenRepository.findByTokenForUpdate("missing")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> refreshTokenService.rotateRefreshToken("missing"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void rotateRefreshTokenShouldRevokeFamilyOnReuseDetection() {
        User user = user();
        RefreshToken old = RefreshToken.builder()
                .token("old")
                .user(user)
                .familyId("family-1")
                .revoked(true)
                .expiryDate(Instant.now().plusSeconds(60))
                .build();

        when(refreshTokenRepository.findByTokenForUpdate("old")).thenReturn(Optional.of(old));

        ApiException ex = assertThrows(ApiException.class,
                () -> refreshTokenService.rotateRefreshToken("old"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        verify(tokenFamilyRevoker).revokeFamily("family-1");
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void rotateRefreshTokenShouldRejectExpiredToken() {
        User user = user();
        RefreshToken old = RefreshToken.builder()
                .token("old")
                .user(user)
                .familyId("family-1")
                .revoked(false)
                .expiryDate(Instant.now().minusSeconds(10))
                .build();

        when(refreshTokenRepository.findByTokenForUpdate("old")).thenReturn(Optional.of(old));

        ApiException ex = assertThrows(ApiException.class,
                () -> refreshTokenService.rotateRefreshToken("old"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void rotateRefreshTokenShouldRevokeOldAndIssueNewInSameFamily() {
        User user = user();
        RefreshToken old = RefreshToken.builder()
                .token("old")
                .user(user)
                .familyId("family-1")
                .revoked(false)
                .expiryDate(Instant.now().plusSeconds(120))
                .build();

        when(refreshTokenRepository.findByTokenForUpdate("old")).thenReturn(Optional.of(old));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken rotated = refreshTokenService.rotateRefreshToken("old");

        assertTrue(old.isRevoked());
        assertEquals("family-1", rotated.getFamilyId());
        assertFalse(rotated.isRevoked());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void revokeTokenForUserShouldRejectForeignTokenOwnership() {
        User owner = user();
        User other = User.builder().id(UUID.randomUUID()).build();

        RefreshToken token = RefreshToken.builder()
                .token("token")
                .user(owner)
                .familyId("family-1")
                .revoked(false)
                .expiryDate(Instant.now().plusSeconds(60))
                .build();

        when(refreshTokenRepository.findByTokenForUpdate("token")).thenReturn(Optional.of(token));

        ApiException ex = assertThrows(ApiException.class,
                () -> refreshTokenService.revokeTokenForUser("token", other));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void revokeTokenForUserShouldMarkTokenRevokedForOwner() {
        User owner = user();

        RefreshToken token = RefreshToken.builder()
                .token("token")
                .user(owner)
                .familyId("family-1")
                .revoked(false)
                .expiryDate(Instant.now().plusSeconds(60))
                .build();

        when(refreshTokenRepository.findByTokenForUpdate("token")).thenReturn(Optional.of(token));

        refreshTokenService.revokeTokenForUser("token", owner);

        assertTrue(token.isRevoked());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void validateRefreshExpirationShouldRejectValueOutsideAllowedRange() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 59_000L);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> refreshTokenService.validateRefreshExpiration());

        assertTrue(ex.getMessage().contains("jwt.refresh-expiration-ms must be between"));
    }

    @Test
    void validateRefreshExpirationShouldAcceptValueInsideAllowedRange() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 86_400_000L);

        refreshTokenService.validateRefreshExpiration();
    }

    @Test
    void revokeAllUserTokensShouldDelegateToRepository() {
        User user = user();

        refreshTokenService.revokeAllUserTokens(user);

        verify(refreshTokenRepository).revokeAllByUser(user);
    }

    @Test
    void rotateRefreshTokenShouldPersistNewTokenWithSameFamilyId() {
        User user = user();
        RefreshToken old = RefreshToken.builder()
                .token("old")
                .user(user)
                .familyId("family-xyz")
                .revoked(false)
                .expiryDate(Instant.now().plusSeconds(300))
                .build();

        when(refreshTokenRepository.findByTokenForUpdate("old")).thenReturn(Optional.of(old));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        refreshTokenService.rotateRefreshToken("old");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());

        RefreshToken newToken = captor.getAllValues().get(1);
        assertEquals("family-xyz", newToken.getFamilyId());
    }

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("user@gp.com")
                .fullName("User")
                .build();
    }
}
