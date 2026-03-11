package com.gp.GP_backend.security;

import com.gp.GP_backend.domain.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Handles JWT access token creation and validation.
 *
 * <p>
 * Token contents (claims):
 * <ul>
 * <li>{@code sub} – user's email (the Spring Security "username")</li>
 * <li>{@code userId} – user's UUID, for fast user lookups without a DB
 * query</li>
 * <li>{@code iat} – issued-at timestamp</li>
 * <li>{@code exp} – expiry timestamp (15 minutes from issue by default)</li>
 * </ul>
 *
 * <p>
 * The signing key is a Base64-encoded HMAC-SHA256 secret defined in
 * {@code application-dev.properties}. In production, load this from a secrets
 * manager.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ─── Token generation ──────────────────────────────────────────────────────

    /**
     * Creates a signed JWT for the given user.
     *
     * <p>
     * Casts {@code UserDetails} to {@link User} to access the UUID.
     * This is safe because our {@link UserDetailsServiceImpl} always returns
     * a {@link User} instance (which implements {@link UserDetails}).
     */
    public String generateToken(UserDetails userDetails) {
        User user = (User) userDetails;

        return Jwts.builder()
                .subject(user.getUsername()) // email
                .claim("userId", user.getId().toString()) // UUID → string
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ─── Claims extraction ─────────────────────────────────────────────────────

    /** Extracts the email (subject) from a token. */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extracts the user UUID embedded in the {@code userId} claim.
     * Use this to avoid a database lookup when you only need the ID.
     */
    public UUID extractUserId(String token) {
        String idStr = parseClaims(token).get("userId", String.class);
        return UUID.fromString(idStr);
    }

    // ─── Validation ────────────────────────────────────────────────────────────

    /**
     * Returns true if the token is cryptographically valid, the subject email
     * matches the provided {@link UserDetails}, and the token has not expired.
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    /**
     * Parses and verifies the token signature, returning the claims payload.
     * Throws a {@link JwtException} if the token is malformed or the signature is
     * invalid.
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Decodes the Base64 secret and builds the HMAC-SHA256 signing key. */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
