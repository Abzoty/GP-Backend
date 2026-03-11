package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.user.entity.RefreshToken;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.RefreshTokenRepository;
import com.gp.GP_backend.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Manages refresh token lifecycle with family-based rotation.
 *
 * <p>
 * <b>Rotation strategy (RFC 6749 / OAuth 2.0 best practice):</b>
 * <ol>
 * <li>On login: Revoke all existing tokens for the user. Issue a new token in a
 * new family.</li>
 * <li>On refresh: Mark the old token as revoked. Issue a new token in the
 * <em>same</em> family.</li>
 * <li>On reuse: If a revoked token is presented, revoke the <em>entire
 * family</em> and reject
 * the request — this indicates a stolen token is being replayed.</li>
 * </ol>
 *
 * <p>
 * <b>Transaction design — why {@link TokenFamilyRevoker} is a separate
 * bean:</b>
 * Spring {@code @Transactional} works through proxies. When a method calls
 * another method
 * on <em>the same object</em> ({@code this.someMethod()}), it bypasses the
 * proxy entirely,
 * meaning {@code @Transactional} annotations on the called method are silently
 * ignored.
 * To guarantee the family revocation commits independently (before the 401
 * exception
 * triggers a rollback of this method's transaction), the revocation lives in
 * {@link TokenFamilyRevoker} — a separate bean with its own proxy and
 * {@code Propagation.REQUIRES_NEW}.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Injected as a separate bean so that its {@code REQUIRES_NEW} transaction
     * propagation is honoured by Spring's proxy. See class-level Javadoc.
     */
    private final TokenFamilyRevoker tokenFamilyRevoker;

    /**
     * Issues a brand-new refresh token for a user after login.
     * All previous tokens for this user are revoked first to enforce single-session
     * semantics.
     * A new {@code familyId} is generated to start a fresh rotation chain.
     */
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

    /**
     * Rotates the refresh token: validates the old one, revokes it, and issues a
     * new one
     * within the same family.
     *
     * <p>
     * If the token is already revoked (reuse detected), delegates to
     * {@link TokenFamilyRevoker#revokeFamily(String)}, which runs in its own
     * independent transaction and commits immediately before this method throws
     * 401.
     * This guarantees the entire family is locked in the DB even though this
     * method's own transaction will be rolled back by the exception.
     *
     * @throws ApiException 401 if the token is unknown, already used, or expired.
     */
    @Transactional
    public RefreshToken rotateRefreshToken(String oldTokenValue) {
        RefreshToken oldToken = refreshTokenRepository.findByToken(oldTokenValue)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        // Reuse detected — a token that was already consumed is being presented again.
        // This likely means a stolen token is being replayed by an attacker.
        if (oldToken.isRevoked()) {
            // Calls through TokenFamilyRevoker's proxy → REQUIRES_NEW transaction
            // commits independently → not rolled back when we throw below.
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

    /**
     * Revokes a specific refresh token (single-device logout).
     * Validates that the token belongs to {@code currentUser} to prevent cross-user
     * revocation.
     *
     * @throws ApiException 401 if token is invalid or doesn't belong to the user.
     */
    @Transactional
    public void revokeTokenForUser(String tokenValue, User currentUser) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (!token.getUser().getId().equals(currentUser.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Token does not belong to the current user");
        }

        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    /**
     * Revokes ALL refresh tokens for a user (logout from all devices).
     * The user must re-authenticate on every device after this call.
     */
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }
}