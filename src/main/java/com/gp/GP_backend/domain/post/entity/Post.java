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

@Entity
@Table(name = "posts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String body;

    /** QUESTION / DISCUSSION */
    @Column(name = "post_type", length = 20)
    @Builder.Default
    private String postType = "QUESTION";

    @Column(name = "is_solved")
    @Builder.Default
    private Boolean isSolved = false;

    /**
     * Circular FK: posts.accepted_answer_id → answers.id
     * Using insertable/updatable=false to let the DB handle the constraint;
     * update this field via a dedicated service method.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_answer_id", foreignKey = @ForeignKey(name = "FK_Post_AcceptedAnswer"))
    private Answer acceptedAnswer;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "good_question_count")
    @Builder.Default
    private Integer goodQuestionCount = 0;

    @Column(length = 500)
    private String tags;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonManagedReference
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Answer> answers = new ArrayList<>();
}
