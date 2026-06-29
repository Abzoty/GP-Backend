package com.gp.GP_backend.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditPostRequest {
    
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 300, message = "Title must be between 3 and 300 characters")
    private String title;

    @NotBlank(message = "Body is required")
    @Size(min = 10, message = "Body must be at least 10 characters")
    private String body;

}
