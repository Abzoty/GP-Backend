package com.gp.GP_backend.domain.post.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a student's answer to a {@link Post} of type QUESTION.
 *
 * <p>{@code upvoteCount} is a denormalised counter incremented by
 * {@code VoteService} — avoids a COUNT query on every feed load.
 *
 * <p>{@code isAccepted} is set by the question author via US-018.
 * When set, {@code Post.acceptedAnswerId} is also updated atomically
 * in {@code PostService}.
 */
@Entity
@Table(name = "answers", indexes = {
        @Index(name = "idx_answers_post_id", columnList = "post_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UNIQUEIDENTIFIER", updatable = false, nullable = false)
    private UUID id;

    /** FK → posts.id */
    @Column(name = "post_id", nullable = false,
            columnDefinition = "UNIQUEIDENTIFIER")
    private UUID postId;

    /** FK → users.id */
    @Column(name = "author_id", nullable = false,
            columnDefinition = "UNIQUEIDENTIFIER")
    private UUID authorId;

    @Column(columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String body;

    /** Denormalised upvote counter — incremented by VoteService. */
    @Column(name = "upvote_count", nullable = false)
    @Builder.Default
    private Integer upvoteCount = 0;

    /** True when the question author marks this as the accepted solution (US-018). */
    @Column(name = "is_accepted", nullable = false)
    @Builder.Default
    private Boolean isAccepted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}