package com.gp.GP_backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Minimal confirmation returned after a successful registration.
 *
 * <p>
 * Intentionally lightweight — the client should call the login endpoint
 * to obtain tokens and the full user profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    private UUID userId;
    private String email;
    private String fullName;
}
