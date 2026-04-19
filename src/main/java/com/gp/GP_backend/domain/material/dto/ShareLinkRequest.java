package com.gp.GP_backend.domain.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class ShareLinkRequest {

    @NotNull(message = "Space ID is required")
    private UUID spaceId;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 1000)
    private String description;

    @NotBlank(message = "URL is required")
    @Size(max = 1024, message = "URL must not exceed 1024 characters")
    private String url;
}