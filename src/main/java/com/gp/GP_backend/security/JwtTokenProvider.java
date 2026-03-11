package com.gp.GP_backend.security;

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

import com.gp.GP_backend.domain.user.entity.User;

/**
 * Stateless JWT utility — generates, validates, and parses access tokens.
 *
 * <h3>Token structure</h3>
 * 
 * <pre>
 * Header : { alg: HS256 }
 * Payload: {
 *   sub  : "user@example.com",   // login username (email)
 *   uid  : "550e8400-...",       // user UUID — avoids a DB lookup per request
 *   iat  : 1700000000,           // issued-at  (Unix seconds)
 *   exp  : 1700000900            // expiry     (iat + expiration-ms)
 * }
 * </pre>
 *
 * <h3>Security notes</h3>
 * <ul>
 * <li>The secret key MUST be a valid Base64 string of at least 32 bytes (256
 * bits)
 * for HS256 to function correctly.</li>
 * <li>The key is decoded once per signing/parsing operation; no global state is
 * kept
 * so key rotation just requires a restart.</li>
 * <li>This class is stateless — token revocation is handled via the
 * {@link com.gp.GP_backend.domain.user.entity.RefreshToken} table.</li>
 * </ul>
 */
@Component
@Slf4j
public class JwtTokenProvider {

    /** Claim key used to embed the user UUID inside the token payload. */
    private static final String CLAIM_USER_ID = "uid";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ── Key derivation ─────────────────────────────────────────────────────────

    /**
     * Decodes the Base64 secret and derives an HMAC-SHA256 signing key.
     *
     * <p>
     * Called on every sign/verify operation (cheap — just byte decoding + key
     * wrap).
     * If the secret is not valid Base64, {@code Decoders.BASE64.decode()} will
     * throw
     * an {@link IllegalArgumentException} at startup time during the first token
     * operation.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Token generation ───────────────────────────────────────────────────────

    /**
     * Generates a signed JWT access token for the given user.
     *
     * <p>
     * The {@code uid} claim embeds the user's UUID so downstream code can avoid
     * an extra database round-trip just to resolve an email to an ID.
     *
     * @param userDetails must be an instance of {@link User} to extract the UUID
     * @return compact, URL-safe JWT string (three Base64url segments separated by
     *         dots)
     */
    public String generateToken(UserDetails userDetails) {
        JwtBuilder builder = Jwts.builder()
                .subject(userDetails.getUsername()) // email
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey());

        // Embed UUID only when we have a full User entity (not a plain UserDetails
        // stub)
        if (userDetails instanceof User user) {
            builder.claim(CLAIM_USER_ID, user.getId().toString());
        }

        return builder.compact();
    }

    // ── Token parsing ──────────────────────────────────────────────────────────

    /**
     * Extracts the email (subject) from a token without validating expiry.
     *
     * @throws JwtException if the token signature is invalid
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extracts the user UUID embedded in the {@code uid} claim.
     *
     * @return the user's UUID, or {@code null} if the claim is absent (e.g. legacy
     *         tokens)
     */
    public UUID extractUserId(String token) {
        String raw = parseClaims(token).get(CLAIM_USER_ID, String.class);
        return (raw != null) ? UUID.fromString(raw) : null;
    }

    // ── Token validation ───────────────────────────────────────────────────────

    /**
     * Full token validation: verifies signature, expiry, and that the subject
     * matches
     * the given {@link UserDetails}.
     *
     * @param token       JWT string from the {@code Authorization: Bearer} header
     * @param userDetails loaded from the database for the extracted email
     * @return {@code true} only if all checks pass
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    /**
     * Parses and verifies the JWT signature, returning the decoded claims payload.
     *
     * @throws JwtException on any signature or format error
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}