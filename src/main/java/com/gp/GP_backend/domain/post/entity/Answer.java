package com.gp.GP_backend.domain.post.entity;

import com.gp.GP_backend.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An answer submitted by a user in response to a {@link Post}.
 *
 * <p>
 * {@code isAccepted} is set to true by the post author when they choose
 * this as the best answer. Only one answer per post can be accepted at a time.
 *
 * <p>
 * {@code upvoteCount} is a denormalized counter updated by
 * {@link com.gp.GP_backend.domain.post.service.VoteService} each time a
 * {@link Vote} is cast or retracted, avoiding expensive COUNT queries.
 */
@Entity
@Table(name = "answers")
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

    /**
     * Back-reference to the parent post.
     * {@code @JsonBackReference} prevents the infinite JSON loop
     * caused by Post → answers → post → answers...
     */
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** Rich text — stored as NVARCHAR(MAX) to support Unicode/Arabic. */
    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String body;

    /** Denormalized upvote count; updated by VoteService. */
    @Column(name = "upvote_count")
    @Builder.Default
    private Integer upvoteCount = 0;

    /** True if the post author accepted this answer. At most one per post. */
    @Column(name = "is_accepted")
    @Builder.Default
    private Boolean isAccepted = false;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
