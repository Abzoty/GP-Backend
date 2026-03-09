package com.gp.GP_backend.domain.post.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AnswerResponse {
    private Long id;
    private Long postId;
    private Long authorId;
    private String authorName;
    private String authorImageUrl;
    private String body;
    private Integer upvoteCount;
    private Boolean isAccepted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
