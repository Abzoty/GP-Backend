package com.gp.GP_backend.domain.space.dto;

import com.gp.GP_backend.domain.space.entity.SpaceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Payload for {@code POST /api/v1/spaces} — creates a new space. */
@Data
public class CreateSpaceRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 1000)
    private String description;

    /**
     * Broad category of the space. Drives the similarity/duplicate check:
     * {@link SpaceCategory#COLLEGE_COURSE} matches by {@code courseCode};
     * all other categories use text-similarity on name + description.
     */
    private SpaceCategory category;

    /**
     * Optional course code to link this space to a specific course (e.g. "CS301").
     * Strongly recommended when {@code category} is
     * {@link SpaceCategory#COLLEGE_COURSE}.
     */
    @Size(max = 30)
    private String courseCode;
}