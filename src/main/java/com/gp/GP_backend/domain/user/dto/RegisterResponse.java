package com.gp.GP_backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal response returned after successful registration.
 *
 * <p>
 * Deliberately excludes tokens — the client must call
 * {@code POST /api/v1/auth/login}
 * to obtain JWT credentials after registration. This keeps the registration
 * endpoint
 * single-responsibility and simplifies email-verification flows in the future.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterResponse {

    /** The newly created user's UUID (as String). */
    private String userId;

    private String email;

    private String fullName;
}