package com.gp.GP_backend.domain.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/** Payload for POST /api/v1/materials — shares a resource in a space. */
@Data
public class CreateMaterialRequest {

    @NotNull
    private UUID spaceId;

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 1000)
    private String description;

    /** PDF / LINK / IMAGE / VIDEO */
    @Pattern(regexp = "PDF|LINK|IMAGE|VIDEO")
    private String resourceType;

    @Size(max = 1024)
    private String url;

    /** File size in KB; null for external links. */
    private Integer fileSizeKb;
}
