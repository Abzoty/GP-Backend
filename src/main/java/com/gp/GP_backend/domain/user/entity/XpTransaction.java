package com.gp.GP_backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "xp_transactions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class XpTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** e.g. POST_CREATED, ANSWER_UPVOTED, DAILY_LOGIN */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "xp_delta", nullable = false)
    private Integer xpDelta;

    @Column(name = "reference_id")
    private Long referenceId;

    /** POST / ANSWER / MATERIAL / LOGIN */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
