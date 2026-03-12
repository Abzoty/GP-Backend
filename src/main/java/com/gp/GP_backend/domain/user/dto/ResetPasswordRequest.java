package com.gp.GP_backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Payload for {@code POST /api/v1/auth/reset-password}. */
@Data
public class ResetPasswordRequest {

    /** The raw token extracted from the reset link sent to the user's email. */
    @NotBlank(message = "Reset token is required")
    private String token;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}