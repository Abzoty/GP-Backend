package com.gp.GP_backend.domain.post.entity;

import com.gp.GP_backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(
    name = "votes",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_vote_user_target",
            columnNames = { "user_id", "target_type", "target_id" })
    },
    indexes = {
        @Index(name = "idx_vote_target", columnList = "target_type,target_id"),
        @Index(name = "idx_vote_user", columnList = "user_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UNIQUEIDENTIFIER", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    

    /**
     * Discriminator for {@code targetId}.
     * Values: {@code ANSWER}, {@code QUESTION}.
     */
    @Column(name = "target_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    /**
     * UUID of the target entity (Answer.id or Post.id).
     * Not a proper FK — enforced at the application layer.
     */
    @Column(name = "target_id", nullable = false, columnDefinition = "UNIQUEIDENTIFIER")
    private UUID targetId;

    /** UPVOTE or GOOD_QUESTION — see class-level Javadoc. */
    @Column(name = "vote_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private VoteType voteType;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
