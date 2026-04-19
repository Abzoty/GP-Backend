package com.gp.GP_backend.domain.material.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for {@code PATCH /api/v1/materials/{id}}.
 *
 * <p>
 * Only {@code title} and {@code description} are editable post-upload.
 * Null fields are ignored (PATCH semantics) — at least one non-null field
 * should be provided for the call to be meaningful.
 */
@Data
public class EditMaterialRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}