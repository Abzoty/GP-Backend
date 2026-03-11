package com.gp.GP_backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned to the client on successful login or token refresh.
 *
 * <p>
 * Contains both the short-lived access token and the long-lived refresh token.
 * The client should store the refresh token securely (HttpOnly cookie preferred
 * over localStorage to mitigate XSS) and use it to renew the access token
 * before it expires.
 *
 * <p>
 * {@code userId} is a String representation of the user's UUID —
 * useful for the frontend to identify the current user without an extra profile
 * call.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    /**
     * JWT Bearer access token. Expires in 15 minutes (configurable via
     * {@code jwt.expiration-ms}).
     */
    private String token;

    /** Token scheme — always "Bearer". */
    @Builder.Default
    private String tokenType = "Bearer";

    /** UUID of the authenticated user (String form for JSON compatibility). */
    private String userId;

    /** Authenticated user's email address. */
    private String email;

    /** Authenticated user's display name. */
    private String fullName;

    /**
     * Opaque refresh token. Valid for 7 days (configurable via
     * {@code jwt.refresh-expiration-ms}).
     * Present this token to {@code POST /api/v1/auth/refresh} to get a new access
     * token.
     */
    private String refreshToken;

    /**
     * Full profile snapshot — populated only on token refresh so the client can
     * update its local user cache without an extra profile request.
     * Null on login (client already has the data from the registration/login flow).
     */
    private UserResponse user;
}