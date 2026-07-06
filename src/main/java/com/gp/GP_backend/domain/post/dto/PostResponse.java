package com.gp.GP_backend.domain.post.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

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