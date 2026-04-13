package com.gp.GP_backend.domain.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for {@code PATCH /api/v1/users/profile}.
 * All fields are optional — only non-null fields will be applied.
 */
@Data
public class UpdateProfileRequest {

    @Size(max = 150)
    private String fullName;

    @Min(1)
    @Max(5)
    private Integer academicYear;

    @Min(1)
    @Max(10)
    private Integer currentSemester;

    private String department;
    private String imageUrl;

    @Size(max = 500)
    private String bio;
}
