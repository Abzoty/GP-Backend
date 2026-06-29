package com.gp.GP_backend.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for US-015 — Answer a question in a space.
 */
@Data
public class CreateAnswerRequest {

    @NotBlank(message = "Answer body is required")
    @Size(min = 3, message = "Answer must be at least 3 characters")
    private String body;
}