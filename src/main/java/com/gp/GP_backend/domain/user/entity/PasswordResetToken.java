package com.gp.GP_backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Stores a single-use, time-limited token for the forgot-password flow.
 *
 * <p>
 * The raw token is never persisted — only its SHA-256 hash is stored,
 * so a database breach cannot be used to trigger password resets.
 * The raw token travels exclusively over email.
 *
 * <p>
 * A {@code BIGINT} identity PK is used intentionally: this is a purely
 * internal table and no other entity holds a FK to it, matching the same
 * decision made for {@link RefreshToken}.
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SHA-256 hex digest of the raw token sent to the user's email.
     * Unique so that a hash collision (astronomically unlikely) is caught at the DB
     * level.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    /** Owning user — cascade delete keeps the table clean if a user is removed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Absolute expiry — 15 minutes from creation is the recommended window. */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * Flipped to {@code true} after a successful password reset to block replay.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** Convenience check mirroring {@link RefreshToken#isExpired()}. */
    public boolean isExpired() {
        return expiryDate.isBefore(Instant.now());
    }
}