package com.gp.GP_backend.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/** Payload for POST /api/v1/answers — submits an answer to a post. */
@Data
public class CreateAnswerRequest {

    @NotNull
    private UUID postId;

    @NotBlank
    private String body;
}
