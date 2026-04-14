package com.gp.GP_backend.domain.space.dto;

import com.gp.GP_backend.domain.space.entity.SpaceCategory;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for {@code PATCH /api/v1/spaces/{spaceId}}.
 * All fields are optional — only non-null values are applied (PATCH semantics).
 * Only members with role {@code ADMIN} in the target space may use this
 * endpoint.
 */
@Data
public class UpdateSpaceRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 1000)
    private String description;

    /** Changing the category does not re-trigger the similarity check. */
    private SpaceCategory category;

    @Size(max = 30)
    private String courseCode;
}