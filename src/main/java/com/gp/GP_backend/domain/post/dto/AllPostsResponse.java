package com.gp.GP_backend.domain.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


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
        private UUID    authorId; 
        private String  authorName;
        private String  authorAvatarUrl;
        private String  body;
        private int     upvoteCount;
        private boolean isAccepted;
        private Instant createdAt;
    }
}