package com.gp.GP_backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable audit log of every XP change for a user.
 *
 * <p>
 * Never updated — only created. This gives a full history of how a user
 * earned or lost XP over time, useful for auditing and analytics.
 *
 * <p>
 * {@code referenceId} is a polymorphic UUID pointing to whichever entity
 * triggered the event (Post, Answer, Material, etc.), identified by
 * {@code referenceType}.
 */
@Entity
@Table(name = "xp_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XpTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UNIQUEIDENTIFIER", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Describes what action earned/lost XP.
     * Examples: {@code POST_CREATED}, {@code ANSWER_UPVOTED}, {@code DAILY_LOGIN}.
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /** Positive = XP earned; negative = XP deducted. */
    @Column(name = "xp_delta", nullable = false)
    private Integer xpDelta;

    /**
     * UUID of the entity that triggered this event (Post, Answer, Material…).
     * Null for events with no associated entity (e.g. DAILY_LOGIN).
     */
    @Column(name = "reference_id", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID referenceId;

    /**
     * Discriminator for {@code referenceId}.
     * Values: {@code POST}, {@code ANSWER}, {@code MATERIAL}, {@code LOGIN}.
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
