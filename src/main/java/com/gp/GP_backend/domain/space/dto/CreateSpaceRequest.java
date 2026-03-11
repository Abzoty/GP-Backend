package com.gp.GP_backend.domain.space.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Payload for POST /api/v1/spaces — creates a new space. */
@Data
public class CreateSpaceRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 1000)
    private String description;

    /** Broad category, e.g. "Computer Science". */
    @Size(max = 80)
    private String category;

    /** Optional course code to link this space to a course (e.g. "CS301"). */
    @Size(max = 30)
    private String courseCode;
}
