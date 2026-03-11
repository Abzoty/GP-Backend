package com.gp.GP_backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned to the client after a successful login or token refresh.
 *
 * <p>
 * Contains both the short-lived access token (JWT) and the long-lived
 * refresh token, along with the authenticated user's profile details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** Short-lived JWT access token. Expires in ~15 minutes. */
    private String token;

    /** Always "Bearer" — tells the client how to attach the token. */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Opaque UUID-string refresh token. Expires in 7 days. */
    private String refreshToken;

    /** Full profile of the authenticated user. */
    private UserResponse user;
}
