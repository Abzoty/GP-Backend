package com.gp.GP_backend.domain.post.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read model returned to the client for a single answer.
 *
 * <p>{@code authorName} is resolved by the service so clients get the
 * display name without a second round-trip.
 */
@Data
@Builder
public class AnswerResponse {

    private UUID id;
    private UUID postId;

    private UUID authorId;
    private String authorName;

    private String body;
    private Integer upvoteCount;
    private Boolean isAccepted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}