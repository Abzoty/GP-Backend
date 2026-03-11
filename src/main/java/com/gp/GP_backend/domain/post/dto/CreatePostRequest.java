package com.gp.GP_backend.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/** Payload for POST /api/v1/posts — creates a new post inside a space. */
@Data
public class CreatePostRequest {

    @NotNull
    private UUID spaceId;

    @NotBlank
    @Size(max = 300)
    private String title;

    @NotBlank
    private String body;

    /** QUESTION (default) or DISCUSSION. */
    @Pattern(regexp = "QUESTION|DISCUSSION")
    private String postType = "QUESTION";

    /** Comma-separated tags, e.g. "java,spring". Max 500 chars. */
    @Size(max = 500)
    private String tags;
}
