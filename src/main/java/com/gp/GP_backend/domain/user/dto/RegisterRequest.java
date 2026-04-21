package com.gp.GP_backend.domain.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Payload for the {@code POST /api/v1/auth/register} endpoint.
 *
 * <p>
 * Bean Validation annotations enforce constraints before the request
 * reaches the service layer. All validation errors are collected and
 * returned together by
 * {@link com.gp.GP_backend.shared.exception.GlobalExceptionHandler}.
 */
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

    /** Optional — not required for registration but must be unique if provided. */
    private String studentId;

    /** 1 = First year … 5 = Fifth year. */
    @Min(1)
    @Max(5)
    private Integer academicYear;

    /** 1 = First semester … 10 = Tenth semester (across all academic years). */
    @Min(1)
    @Max(10)
    private Integer currentSemester;
}
