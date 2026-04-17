package com.gp.GP_backend.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EditPostRequest {
    
    @NotBlank(message = "Title is required")
    @Size(min = 10, max = 300, message = "Title must be between 10 and 300 characters")
    private String title;

    @NotBlank(message = "Body is required")
    @Size(min = 30, message = "Body must be at least 30 characters")
    private String body;

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }
}
