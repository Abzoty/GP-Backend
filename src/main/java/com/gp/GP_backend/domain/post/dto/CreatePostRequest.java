package com.gp.GP_backend.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for US-014 — Create a question post in a space.
 *
 * <p>Validation rules from the acceptance criteria:
 * <ul>
 *   <li>Title: 3–300 characters</li>
 *   <li>Body: minimum 10 characters</li>
 *   <li>Tags: max 5, each tag max 30 characters</li>
 * </ul>
 */
@Data
public class CreatePostRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 300, message = "Title must be between 3 and 300 characters")
    private String title;

    @NotBlank(message = "Body is required")
    @Size(min = 10, message = "Body must be at least 10 characters")
    private String body;
}