package com.gp.GP_backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Payload for {@code POST /api/v1/auth/reset-password}. */
@Data
public class ResetPasswordRequest {

    /** The raw token extracted from the reset link sent to the user's email. */
    @NotBlank(message = "Reset token is required")
    private String token;

    @NotBlank
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$",
            message = "Password must contain uppercase, lowercase, number, and special character")
    private String newPassword;
}