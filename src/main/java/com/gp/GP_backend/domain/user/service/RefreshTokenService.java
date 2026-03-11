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

/**
 * Manages the lifecycle of refresh tokens: creation, rotation, and revocation.
 *
 * <h3>Token-family model (RFC-style)</h3>
 * <ul>
 * <li><b>Login</b> → new token + new family ID.</li>
 * <li><b>Refresh</b> → old token revoked, new token issued in the same
 * family.</li>
 * <li><b>Reuse detected</b> (revoked token presented) → entire family revoked,
 * forcing re-login on all sessions from that login chain.</li>
 * <li><b>Logout single device</b> → revoke the presented token only.</li>
 * <li><b>Logout all devices</b> → revoke all tokens for the user.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    /**
     * Refresh token TTL, injected from {@code jwt.refresh-expiration-ms} property.
     */
    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    // ── Creation ───────────────────────────────────────────────────────────────

    /**
     * Creates a brand-new refresh token for the given user (called on every
     * successful login).
     *
     * <p>
     * A new {@link RefreshToken#getFamilyId() familyId} is generated per login so
     * that
     * multiple concurrent device sessions are tracked independently.
     *
     * @param user the authenticated user
     * @return the persisted {@link RefreshToken}
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString()) // opaque random value
                .familyId(UUID.randomUUID().toString()) // new family per login session
                .revoked(false)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        return refreshTokenRepository.save(token);
    }

    // ── Rotation ───────────────────────────────────────────────────────────────

    /**
     * Validates the old token and issues a new one in the same family (token
     * rotation).
     *
     * <p>
     * Rotation ensures that a stolen refresh token is usable only once — if the
     * attacker rotates it first, the legitimate user's next request will trigger
     * the
     * reuse-detection path and invalidate all sessions in that family.
     *
     * @param oldTokenValue the raw token string from the client's request body
     * @return the newly issued {@link RefreshToken}
     * @throws IllegalArgumentException on invalid, revoked, or expired tokens
     */
    @Transactional
    public RefreshToken rotateRefreshToken(String oldTokenValue) {
        RefreshToken oldToken = refreshTokenRepository.findByToken(oldTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // ── Reuse detection ────────────────────────────────────────────────────
        // A revoked token being presented again means either:
        // (a) the client has a bug and is re-sending an old token, or
        // (b) an attacker stole the token and rotated it before the legitimate user.
        // Either way, revoke the entire family to force re-login.
        if (oldToken.isRevoked()) {
            refreshTokenRepository.revokeAllByFamilyId(oldToken.getFamilyId());
            throw new IllegalArgumentException(
                    "Refresh token reuse detected. All sessions in this login have been " +
                            "invalidated for your security. Please log in again.");
        }

        if (oldToken.isExpired()) {
            throw new IllegalArgumentException("Refresh token has expired. Please log in again.");
        }

        // Revoke the old token (makes it a "used" token)
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        // Issue a replacement token in the same family
        RefreshToken newToken = RefreshToken.builder()
                .user(oldToken.getUser())
                .token(UUID.randomUUID().toString())
                .familyId(oldToken.getFamilyId()) // same family = same device session
                .revoked(false)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        return refreshTokenRepository.save(newToken);
    }

    // ── Revocation ─────────────────────────────────────────────────────────────

    /**
     * Logs out from the current device by revoking the presented refresh token.
     *
     * @param tokenValue the raw token string to revoke
     * @throws IllegalArgumentException if the token does not exist
     */
    @Transactional
    public void revokeToken(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    /**
     * Logs out from ALL devices by revoking every refresh token for a user.
     *
     * @param user the user whose tokens should all be revoked
     */
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    /**
     * Resolves the {@link User} from a raw refresh token string.
     *
     * <p>
     * Used by the /logout-all endpoint to identify the account without
     * performing a full rotation (wasteful when we are about to revoke everything
     * anyway).
     *
     * @param tokenValue the raw token string
     * @return the owning {@link User}
     * @throws IllegalArgumentException if the token does not exist
     */
    @Transactional(readOnly = true)
    public User getUserFromToken(String tokenValue) {
        return refreshTokenRepository.findByToken(tokenValue)
                .map(t -> (User) t.getUser())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
    }
}