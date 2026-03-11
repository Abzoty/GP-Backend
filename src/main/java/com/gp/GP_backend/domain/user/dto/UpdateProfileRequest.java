package com.gp.GP_backend.domain.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

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

    @DecimalMin("0.0")
    @DecimalMax("4.0")
    private BigDecimal gpa;

    private String department;
    private String imageUrl;

    @Size(max = 500)
    private String bio;
}
