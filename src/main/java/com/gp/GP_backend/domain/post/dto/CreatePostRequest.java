package com.gp.GP_backend.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class CreatePostRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 300, message = "Title must be between 3 and 300 characters")
    private String title;

    @NotBlank(message = "Body is required")
    @Size(min = 3, message = "Body must be at least 3 characters")
    private String body;
}