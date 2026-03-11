package com.gp.GP_backend.domain.user.controller;

import com.gp.GP_backend.domain.user.dto.*;
import com.gp.GP_backend.domain.user.entity.RefreshToken;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.service.RefreshTokenService;
import com.gp.GP_backend.domain.user.service.UserService;
import com.gp.GP_backend.security.JwtTokenProvider;
import com.gp.GP_backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Handles user authentication: registration, login, token refresh, and logout.
 *
 * <p>
 * All endpoints under {@code /api/v1/auth/**} are publicly accessible
 * (configured in {@link com.gp.GP_backend.config.SecurityConfig}).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

        private final UserService userService;
        private final RefreshTokenService refreshTokenService;
        private final AuthenticationManager authenticationManager;
        private final JwtTokenProvider jwtTokenProvider;
        private final ModelMapper modelMapper;

        // ── Register ───────────────────────────────────────────────────────────────

        /**
         * Creates a new user account.
         *
         * <p>
         * Returns 201 Created on success. The response body contains only the
         * non-sensitive confirmation data — the client must call {@code /login} to get
         * tokens.
         *
         * @param request validated registration fields (email, password, name, etc.)
         */
        @PostMapping("/register")
        public ResponseEntity<ApiResponse<RegisterResponse>> register(
                        @Valid @RequestBody RegisterRequest request) {

                User user = userService.registerUser(request);

                RegisterResponse body = RegisterResponse.builder()
                                .userId(user.getId().toString())
                                .email(user.getEmail())
                                .fullName(user.getFullName())
                                .build();

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.ok("Registration successful", body));
        }

        // ── Login ──────────────────────────────────────────────────────────────────

        /**
         * Authenticates the user and issues a short-lived JWT access token plus a
         * longer-lived refresh token.
         *
         * <p>
         * The {@link AuthenticationManager} delegates to
         * {@link com.gp.GP_backend.config.SecurityConfig#authenticationProvider()},
         * which uses BCrypt to verify the password. Spring Security throws
         * {@link org.springframework.security.authentication.BadCredentialsException}
         * on wrong credentials — caught by
         * {@link com.gp.GP_backend.shared.exception.GlobalExceptionHandler}.
         *
         * @param request login credentials (email + password)
         */
        @PostMapping("/login")
        public ResponseEntity<ApiResponse<AuthResponse>> login(
                        @Valid @RequestBody LoginRequest request) {

                // Throws BadCredentialsException if email/password don't match
                Authentication auth = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getEmail(), request.getPassword()));

                User user = (User) auth.getPrincipal();
                String accessToken = jwtTokenProvider.generateToken(user);
                String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

                AuthResponse body = AuthResponse.builder()
                                .token(accessToken)
                                .tokenType("Bearer")
                                .refreshToken(refreshToken)
                                .userId(user.getId().toString())
                                .email(user.getEmail())
                                .fullName(user.getFullName())
                                .build();

                return ResponseEntity.ok(ApiResponse.ok("Login successful", body));
        }

        // ── Token Refresh ──────────────────────────────────────────────────────────

        /**
         * Issues a new access token and rotates the refresh token.
         *
         * <p>
         * The old refresh token is revoked immediately after this call —
         * it must not be reused. The client should store the new refresh token
         * returned in the response body.
         *
         * @param request body containing the current refresh token string
         */
        @PostMapping("/refresh")
        public ResponseEntity<ApiResponse<AuthResponse>> refresh(
                        @Valid @RequestBody RefreshRequest request) {

                // Rotate: validate old token, revoke it, issue a new one in the same family
                RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(
                                request.getRefreshToken());

                User user = (User) newRefreshToken.getUser();
                String newAccessToken = jwtTokenProvider.generateToken(user);

                AuthResponse body = AuthResponse.builder()
                                .token(newAccessToken)
                                .tokenType("Bearer")
                                .refreshToken(newRefreshToken.getToken())
                                .userId(user.getId().toString())
                                .email(user.getEmail())
                                .fullName(user.getFullName())
                                .user(modelMapper.map(user, UserResponse.class))
                                .build();

                return ResponseEntity.ok(ApiResponse.ok("Token refreshed", body));
        }

        // ── Logout ─────────────────────────────────────────────────────────────────

        /**
         * Logs out from the current device by revoking the provided refresh token.
         *
         * <p>
         * <b>BUG FIX:</b> The previous implementation had {@code @Valid @RequestBody}
         * and {@code @AuthenticationPrincipal} on the same parameter — these are
         * mutually
         * exclusive. {@code @RequestBody} deserializes JSON into a DTO;
         * {@code @AuthenticationPrincipal} injects the security context principal.
         * The logout now correctly accepts the refresh token in the request body.
         *
         * @param request body containing the refresh token to revoke
         */
        @PostMapping("/logout")
        public ResponseEntity<ApiResponse<Void>> logout(
                        @Valid @RequestBody RefreshRequest request) {

                refreshTokenService.revokeToken(request.getRefreshToken());
                return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
        }

        /**
         * Logs out from ALL devices by revoking every refresh token for the account
         * associated with the provided token.
         *
         * @param request body containing any valid refresh token belonging to the user
         */
        @PostMapping("/logout-all")
        public ResponseEntity<ApiResponse<Void>> logoutAll(
                        @Valid @RequestBody RefreshRequest request) {

                // Resolve the owning user, then revoke all their sessions
                User user = refreshTokenService.getUserFromToken(request.getRefreshToken());
                refreshTokenService.revokeAllUserTokens(user);

                return ResponseEntity.ok(ApiResponse.ok("Logged out from all devices", null));
        }
}