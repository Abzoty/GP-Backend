package com.gp.GP_backend.domain.notification.entity;

import com.gp.GP_backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An in-app notification delivered to a user.
 *
 * <p>
 * Notifications can be system-generated (sender = null) or triggered by
 * another user's action (e.g. "Alice answered your question").
 *
 * <p>
 * {@code referenceType} + {@code referenceId} form a polymorphic link to
 * the entity that caused the notification (a Post, Answer, Material, or Space).
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UNIQUEIDENTIFIER", updatable = false, nullable = false)
    private UUID id;

    /** The user who will see this notification. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    /**
     * The user whose action triggered this notification.
     * Null for system notifications (e.g. "You levelled up!").
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    /**
     * Machine-readable type used by the frontend to decide which icon/template to
     * render.
     * Examples: {@code NEW_ANSWER}, {@code ANSWER_ACCEPTED},
     * {@code UPVOTE_RECEIVED}.
     */
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String message;

    /**
     * Discriminator for {@code referenceId}.
     * Values: {@code POST}, {@code ANSWER}, {@code MATERIAL}, {@code SPACE}.
     */
    @Column(name = "reference_type", length = 30)
    private String referenceType;

    /**
     * UUID of the related entity. Null for notifications with no specific entity
     * context.
     */
    @Column(name = "reference_id", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID referenceId;

    /**
     * False until the user opens the notification; used for the unread badge count.
     */
    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
