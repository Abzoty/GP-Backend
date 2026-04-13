package com.gp.GP_backend.domain.post.entity;

import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A post is the primary content unit within a {@link Space}.
 *
 * <p>
 * Two types:
 * <ul>
 * <li>{@code QUESTION} – expects a single accepted answer; has an
 * {@code is_solved} flag.</li>
 * <li>{@code DISCUSSION} – open-ended; no accepted answer concept.</li>
 * </ul>
 *
 * <p>
 * <b>Circular FK note:</b> {@code posts.accepted_answer_id} →
 * {@code answers.id} creates
 * a circular dependency with {@code answers.post_id} → {@code posts.id}.
 * Hibernate resolves this by inserting the post first (accepted_answer_id =
 * NULL),
 * then inserting the answer, then updating the post. The DB constraint is named
 * {@code FK_Post_AcceptedAnswer} to make schema diffs readable.
 */
@Entity
@Table(name = "posts")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 300)
    private String title;

    /** Rich text body — stored as NVARCHAR(MAX) to support Unicode/Arabic. */
    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String body;

    /** QUESTION or DISCUSSION — see class-level Javadoc. */
    @Column(name = "post_type", length = 20)
    @Builder.Default
    private String postType = "QUESTION";

    /** True once the author has accepted an answer (QUESTION type only). */
    @Column(name = "is_solved")
    @Builder.Default
    private Boolean isSolved = false;

    /**
     * The answer chosen by the post author as the correct/best answer.
     * Null until explicitly set. Changing this also updates {@code isSolved}.
     * See the named FK constraint note in the class Javadoc.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_answer_id", foreignKey = @ForeignKey(name = "FK_Post_AcceptedAnswer"))
    private Answer acceptedAnswer;

    /** Incremented each time the post detail page is loaded. */
    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    /** Number of "Good Question" votes received. */
    @Column(name = "good_question_count")
    @Builder.Default
    private Integer goodQuestionCount = 0;

    /** Comma-separated tags (e.g. "java,spring,jpa"). Max 500 chars. */
    @Column(length = 500)
    private String tags;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Owned answers for this post. CascadeType.ALL ensures answers are deleted
     * when their post is deleted. {@code @JsonManagedReference} prevents
     * infinite JSON recursion when entities are serialised directly.
     */
    @JsonManagedReference
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Answer> answers = new ArrayList<>();
}
