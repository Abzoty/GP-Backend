package com.gp.GP_backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Size(min = 8, max = 64, message = "New password must be between 8 and 64 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$",
            message = "New password must contain uppercase, lowercase, number, and special character")
    private String newPassword;
}