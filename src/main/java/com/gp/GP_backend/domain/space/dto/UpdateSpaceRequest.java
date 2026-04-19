package com.gp.GP_backend.domain.space.dto;

import com.gp.GP_backend.domain.space.entity.SpaceCategory;
import jakarta.validation.constraints.Size;
import lombok.Data;

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