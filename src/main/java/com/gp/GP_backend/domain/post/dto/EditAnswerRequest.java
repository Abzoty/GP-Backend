package com.gp.GP_backend.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EditAnswerRequest {
    @NotBlank(message = "Answer body is required")
    @Size(min = 10, message = "Answer must be at least 10 characters")
    String body;


    public String getBody() {
        return body;
    }
}
