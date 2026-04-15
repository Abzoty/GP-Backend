package com.gp.GP_backend.domain.post.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a question or discussion post inside a Space.
 *
 * <p>Primary key uses Hibernate's UUID strategy (generated before INSERT),
 * consistent with the rest of the project's entities.
 *
 * <p>Tags are stored as a comma-separated string in a single column
 * (max 5 tags enforced at the service layer).
 */
@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_space_created", columnList = "space_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UNIQUEIDENTIFIER", updatable = false, nullable = false)
    private UUID id;

    /** FK → spaces.id — not a JPA association to keep cross-domain coupling minimal. */
    @Column(name = "space_id", nullable = false,
            columnDefinition = "UNIQUEIDENTIFIER")
    private UUID spaceId;

    /** FK → users.id */
    @Column(name = "author_id", nullable = false,
            columnDefinition = "UNIQUEIDENTIFIER")
    private UUID authorId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String body;


    @Column(name = "is_solved", nullable = false)
    @Builder.Default
    private Boolean isSolved = false;

    /** Set when a question author accepts an answer. */
    @Column(name = "accepted_answer_id",
            columnDefinition = "UNIQUEIDENTIFIER")
    private UUID acceptedAnswerId;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    /** Equivalent of "upvotes" on the question itself (Good Question feature). */
    @Column(name = "good_question_count", nullable = false)
    @Builder.Default
    private Integer goodQuestionCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}