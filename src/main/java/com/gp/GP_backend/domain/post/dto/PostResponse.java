package com.gp.GP_backend.domain.post.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read model returned to the client for a single post.
 *
 * <p>{@code answerCount} is populated from a COUNT query in the service
 * rather than a JPA collection, avoiding the N+1 trap on feed pages.
 *
 * <p>{@code authorName} is resolved by the service from {@code UserService}
 * so the client never needs a second request.
 */
@Data
@Builder
public class PostResponse {

    private UUID id;
    private UUID spaceId;

    private UUID authorId;
    private String authorName;

    private String title;
    private String body;

    private Boolean isSolved;
    private UUID acceptedAnswerId;

    private Integer viewCount;
    private Integer goodQuestionCount;
    private Integer answerCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}