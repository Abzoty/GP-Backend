package com.gp.GP_backend.domain.post.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

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