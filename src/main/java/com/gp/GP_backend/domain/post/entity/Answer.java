package com.gp.GP_backend.domain.post.entity;

import com.gp.GP_backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

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
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "post_id", nullable = false)
        private Post post;

        /** FK → users.id */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "author_id", nullable = false)
        private User author;

        @Column(columnDefinition = "NVARCHAR(MAX)", nullable = false)
        private String body;

        @Column(name = "upvote_count", nullable = false)
        @Builder.Default
        private Integer upvoteCount = 0;

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