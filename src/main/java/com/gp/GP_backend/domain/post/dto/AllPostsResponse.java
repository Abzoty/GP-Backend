package com.gp.GP_backend.domain.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO returned for each post in the paginated feed (US-013).
 *
 * Acceptance criteria: each card shows title, author, vote count,
 * answer count, and tags. Sorted by newest (default) or top votes.
 * The full body is intentionally omitted — it belongs in PostDetailResponse.
 */
// AllPostsResponse.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllPostsResponse {

    private UUID   postId;
    private String title;
    private String body;

    private UUID   authorId;
    private String authorName;
    private String authorAvatarUrl;

    private UUID   spaceId;
    private String spaceName;

    private int     goodQuestionCount;
    private int     answerCount;
    private int     viewCount;
    private boolean solved;

    private List<AnswerSummary> top3Answers;

    private Instant createdAt;
    private Instant updatedAt;

    // --- Nested DTO ---
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerSummary {
        private UUID    answerId;
        private UUID    authorId;   // was Long — matches Answer.authorId which is UUID
        private String  authorName;
        private String  authorAvatarUrl;
        private String  body;
        private int     upvoteCount;
        private boolean isAccepted;
        private Instant createdAt;
    }
}