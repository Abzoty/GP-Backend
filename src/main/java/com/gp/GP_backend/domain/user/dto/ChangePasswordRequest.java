package com.gp.GP_backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for {@code POST /api/v1/users/change-password} (authenticated users
 * only).
 */
@Data
public class ChangePasswordRequest {

    /**
     * Must match the user's current BCrypt-hashed password — prevents JWT-theft
     * lockouts.
     */
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank
    @Size(min = 8, message = "New password must be at least 8 characters")
    private String newPassword;
}