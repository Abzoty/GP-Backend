package com.gp.GP_backend.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Payload for {@code POST /api/v1/auth/forgot-password}. */
@Data
public class ForgotPasswordRequest {

    @NotBlank
    @Email(message = "Must be a valid email address")
    private String email;
}