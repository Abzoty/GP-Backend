package com.gp.GP_backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Persistent refresh token used for JWT rotation.
 *
 * <p>
 * <b>Family-based reuse detection:</b> Every login creates a new
 * {@code familyId}.
 * On each refresh, the old token is revoked and a new one is issued in the same
 * family.
 * If a revoked token is presented, the entire family is revoked — forcing
 * re-login.
 * This detects token theft without storing a full token history.
 *
 * <p>
 * The primary key remains a {@code Long} IDENTITY since this is a purely
 * internal table
 * with no external references (no other entity has a FK to refresh_tokens.id).
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The opaque token string sent to the client (UUID v4 string). */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * The user this token belongs to.
     *
     * <p>
     * <b>Why EAGER:</b> Every operation on a refresh token (rotate, revoke,
     * validate)
     * immediately needs the associated user — to generate a new JWT, to verify
     * ownership,
     * or to check credentials. Using LAZY here causes a
     * {@code LazyInitializationException}
     * when the service method's {@code @Transactional} boundary closes before the
     * controller accesses {@code token.getUser()}. 
     * EAGER avoids an extra query in practice
     * because
     * {@code findByToken()} always retrieves the user in the same join anyway.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Groups all tokens issued in a single login session.
     * If reuse is detected, all tokens sharing this family ID are revoked.
     */
    @Column(nullable = false)
    private String familyId;

    /** True if this token has been used (rotated) or explicitly revoked. */
    @Column(nullable = false)
    private boolean revoked;

    /** Absolute expiry — checked regardless of the revoked flag. */
    @Column(nullable = false)
    private Instant expiryDate;

    /**
     * Convenience check used in
     * {@link com.gp.GP_backend.domain.user.service.RefreshTokenService}.
     */
    public boolean isExpired() {
        return expiryDate.isBefore(Instant.now());
    }
}