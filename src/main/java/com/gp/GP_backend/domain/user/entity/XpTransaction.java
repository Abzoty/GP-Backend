package com.gp.GP_backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "xp_transactions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_xp_transactions_event_key", columnNames = "event_key")
    },
    indexes = {
        @Index(name = "idx_xp_transactions_user_event", columnList = "user_id,event_type"),
        @Index(name = "idx_xp_transactions_created_at", columnList = "created_at")
    })
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


    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_key", nullable = false, length = 200)
    private String eventKey;

    @Column(name = "xp_delta", nullable = false)
    private Integer xpDelta;

    @Column(name = "reference_id", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
