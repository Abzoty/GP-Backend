package com.gp.GP_backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Payload for {@code POST /api/v1/auth/refresh}. */
@Data
public class RefreshRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
