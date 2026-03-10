package com.gp.GP_backend.domain.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    private String fullName;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String studentId;

    @Min(1) @Max(5)
    private Integer academicYear;

    @Min(1) @Max(10)
    private Integer currentSemester;
}