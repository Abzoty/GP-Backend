package com.gp.GP_backend.domain.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateMaterialRequest {

    @NotNull
    private UUID spaceId;

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 1000)
    private String description;

    @NotBlank
    @Size(max = 10, message = "Resource type must not exceed 10 characters")
    private String resourceType;

    @Size(max = 1024)
    private String url;

    /** File size in KB; null for external links. */
    private Integer fileSizeKb;
}
