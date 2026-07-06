package com.gp.GP_backend.domain.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;


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

    @Size(max = 100)
    private String department;

    @Size(max = 512)
    private String imageUrl;

    @Size(max = 500)
    private String bio;
}
