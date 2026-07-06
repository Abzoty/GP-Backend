package com.gp.GP_backend.domain.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$",
            message = "Password must contain uppercase, lowercase, number, and special character")
    private String password;

    @Pattern(
        regexp = "^\\d{8}$",
        message = "Student ID must be exactly 8 digits"
    )
    private String studentId;

    @Min(1)
    @Max(4)
    private Integer academicYear;

    @Min(1)
    @Max(2)
    private Integer currentSemester;
}
