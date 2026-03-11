package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.user.entity.RefreshToken;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    // Called on login — starts a new family
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .familyId(UUID.randomUUID().toString())  // new family per login
                .revoked(false)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        return refreshTokenRepository.save(token);
    }

    // Called on refresh — rotates the token within the same family
    @Transactional
    public RefreshToken rotateRefreshToken(String oldTokenValue) {
        RefreshToken oldToken = refreshTokenRepository.findByToken(oldTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // Reuse detection — token was already used (rotated) before
        if (oldToken.isRevoked()) {
            // Someone is reusing an old token — possible theft
            // Revoke the entire family to force re-login on all devices in this session
            refreshTokenRepository.revokeAllByFamilyId(oldToken.getFamilyId());
            throw new IllegalArgumentException(
                "Refresh token reuse detected. All sessions invalidated. Please login again.");
        }

        if (oldToken.isExpired()) {
            throw new IllegalArgumentException("Refresh token expired. Please login again.");
        }

        // Invalidate the old token
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        // Issue a new token in the same family
        RefreshToken newToken = RefreshToken.builder()
                .user(oldToken.getUser())
                .token(UUID.randomUUID().toString())
                .familyId(oldToken.getFamilyId())    // ← same family
                .revoked(false)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        return refreshTokenRepository.save(newToken);
    }

    // Logout from current device only
    @Transactional
    public void revokeToken(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    // Logout from ALL devices
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }
}