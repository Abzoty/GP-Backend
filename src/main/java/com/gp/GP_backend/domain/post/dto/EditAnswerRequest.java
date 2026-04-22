package com.gp.GP_backend.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditAnswerRequest {
    @NotBlank(message = "Answer body is required")
    @Size(min = 10, message = "Answer must be at least 10 characters")
    private String body;
}
