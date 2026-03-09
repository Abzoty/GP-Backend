package com.gp.GP_backend.domain.post.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PostResponse {
    private Long id;
    private Long spaceId;
    private String spaceName;
    private Long authorId;
    private String authorName;
    private String authorImageUrl;
    private String title;
    private String body;
    private String postType;
    private Boolean isSolved;
    private Long acceptedAnswerId;
    private Integer viewCount;
    private Integer goodQuestionCount;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer answerCount;
    private List<AnswerResponse> answers; // populated only on detail view
}
