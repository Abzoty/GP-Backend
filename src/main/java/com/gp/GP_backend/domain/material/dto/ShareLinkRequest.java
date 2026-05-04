package com.gp.GP_backend.domain.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

import org.hibernate.validator.constraints.URL;

@Data
public class ShareLinkRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 1000)
    private String description;

    @URL(message = "Invalid URL format")
    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^(https?:\\/\\/)(www\\.)?[a-zA-Z0-9\\-._~%]+(\\.[a-zA-Z0-9\\-._~%]+)+(:\\d+)?(\\/[^\\s]*)?$", message = "Please enter a valid URL starting with http:// or https://")
    @Size(max = 1024, message = "URL must not exceed 1024 characters")
    @Pattern(regexp = "^https?://.+$", message = "URL must start with http:// or https://")
    private String url;
}