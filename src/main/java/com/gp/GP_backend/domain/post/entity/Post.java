package com.gp.GP_backend.domain.post.entity;

import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a question or discussion post inside a Space.
 *
 * <p>
 * {@code space} and {@code author} are real JPA {@code @ManyToOne}
 * associations (rather than bare UUID columns) so the FK relationships to
 * {@code spaces} and {@code users} actually show up in the generated
 * schema/ERD and Hibernate can enforce referential integrity.
 *
 * <p>
 * Tags are stored as a comma-separated string in a single column
 * (max 5 tags enforced at the service layer).
 */
@Entity
@Table(name = "posts", indexes = {
                @Index(name = "idx_posts_space_created", columnList = "space_id, created_at"),
                @Index(name = "idx_posts_space_solved", columnList = "space_id, is_solved"),
                @Index(name = "idx_posts_space_votes", columnList = "space_id, good_question_count DESC")
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

        /** FK → spaces.id */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "space_id", nullable = false)
        private Space space;

        /** FK → users.id */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "author_id", nullable = false)
        private User author;

        @Column(nullable = false, length = 300)
        private String title;

        @Column(columnDefinition = "NVARCHAR(MAX)", nullable = false)
        private String body;

        @Column(name = "is_solved", nullable = false)
        @Builder.Default
        private Boolean isSolved = false;

        /** Set when a question author accepts an answer. */
        @Column(name = "accepted_answer_id", columnDefinition = "UNIQUEIDENTIFIER")
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