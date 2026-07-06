package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.user.entity.RefreshToken;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.RefreshTokenRepository;
import com.gp.GP_backend.shared.exception.ApiException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final long MIN_REFRESH_EXPIRATION_MS = 60_000L; // 1 minute
    private static final long MAX_REFRESH_EXPIRATION_MS = 2_592_000_000L; // 30 days

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    private final TokenFamilyRevoker tokenFamilyRevoker;

    @PostConstruct
    void validateRefreshExpiration() {
        if (refreshExpirationMs < MIN_REFRESH_EXPIRATION_MS || refreshExpirationMs > MAX_REFRESH_EXPIRATION_MS) {
            throw new IllegalStateException(
                    "jwt.refresh-expiration-ms must be between "
                            + MIN_REFRESH_EXPIRATION_MS
                            + " and "
                            + MAX_REFRESH_EXPIRATION_MS
                            + " milliseconds");
        }
    }


    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Revoke all old tokens before issuing a new one (prevents session
        // accumulation)
        refreshTokenRepository.revokeAllByUser(user);
        refreshTokenRepository.flush(); // ensure revocation is written before the new insert

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString()) // opaque token string
                .familyId(UUID.randomUUID().toString()) // new family = new login session
                .revoked(false)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        return refreshTokenRepository.save(token);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(String oldTokenValue) {
        RefreshToken oldToken = refreshTokenRepository.findByTokenForUpdate(oldTokenValue)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (oldToken.isRevoked()) {
            tokenFamilyRevoker.revokeFamily(oldToken.getFamilyId());
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Refresh token reuse detected. All sessions have been invalidated. Please log in again.");
        }

        if (oldToken.isExpired()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token has expired. Please log in again.");
        }

        // Consume the old token
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        // Issue a new token in the same family
        RefreshToken newToken = RefreshToken.builder()
                .user(oldToken.getUser())
                .token(UUID.randomUUID().toString())
                .familyId(oldToken.getFamilyId()) // same family = same login session
                .revoked(false)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        return refreshTokenRepository.save(newToken);
    }

    @Transactional
    public void revokeTokenForUser(String tokenValue, User currentUser) {
        RefreshToken token = refreshTokenRepository.findByTokenForUpdate(tokenValue)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (!token.getUser().getId().equals(currentUser.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Token does not belong to the current user");
        }

        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }


    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }
}