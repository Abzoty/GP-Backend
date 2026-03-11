package com.gp.GP_backend.domain.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Read-only representation of a Post returned to the client. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {
    private UUID id;
    private UUID spaceId;
    private String spaceName;
    private UUID authorId;
    private String authorName;
    private String authorImageUrl;
    private String title;
    private String body;
    private String postType;
    private Boolean isSolved;
    private UUID acceptedAnswerId;
    private Integer viewCount;
    private Integer goodQuestionCount;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** Total answer count (always populated). */
    private Integer answerCount;
    /**
     * Full answer list — only populated on the post detail endpoint, null on list
     * endpoints.
     */
    private List<AnswerResponse> answers;
}
