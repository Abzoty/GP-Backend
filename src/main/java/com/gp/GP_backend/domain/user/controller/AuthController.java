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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Handles user registration, login, token refresh, and logout operations.
 *
 * <p>
 * All endpoints under {@code /api/v1/auth} are public (no JWT required).
 * See {@link com.gp.GP_backend.config.SecurityConfig} for the permit-list.
 *
 * <p>
 * Validation failures and business-logic exceptions are handled centrally by
 * {@link com.gp.GP_backend.shared.exception.GlobalExceptionHandler} — no
 * try/catch
 * needed here.
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

        /**
         * Registers a new user account.
         *
         * <p>
         * Returns 201 CREATED with a minimal confirmation payload.
         * The client should call {@code /login} to obtain tokens and the full profile.
         */
        @PostMapping("/register")
        public ResponseEntity<ApiResponse<RegisterResponse>> register(
                        @Valid @RequestBody RegisterRequest request) {

                User user = userService.registerUser(request);

                RegisterResponse body = RegisterResponse.builder()
                                .userId(user.getId())
                                .email(user.getEmail())
                                .fullName(user.getFullName())
                                .build();

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.ok("Registration successful", body));
        }

        /**
         * Authenticates the user and returns a JWT access token and refresh token.
         *
         * <p>
         * Spring Security's {@link AuthenticationManager} validates the credentials.
         * On success, the authenticated {@link User} is returned as the principal.
         */
        @PostMapping("/login")
        public ResponseEntity<ApiResponse<AuthResponse>> login(
                        @Valid @RequestBody LoginRequest request) {

                // Delegate credential validation to Spring Security; throws
                // BadCredentialsException on failure
                Authentication auth = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

                User user = (User) auth.getPrincipal();

                String accessToken = jwtTokenProvider.generateToken(user);
                String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

                AuthResponse body = AuthResponse.builder()
                                .token(accessToken)
                                .tokenType("Bearer")
                                .refreshToken(refreshToken)
                                .user(modelMapper.map(user, UserResponse.class))
                                .build();

                return ResponseEntity.ok(ApiResponse.ok("Login successful", body));
        }

        /**
         * Exchanges a valid refresh token for a new access token + rotated refresh
         * token.
         *
         * <p>
         * The old refresh token is invalidated upon use (rotation). Presenting an
         * already-used token triggers full session revocation.
         */
        @PostMapping("/refresh")
        public ResponseEntity<ApiResponse<AuthResponse>> refresh(
                        @Valid @RequestBody RefreshRequest request) {

                RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(request.getRefreshToken());
                User user = (User) newRefreshToken.getUser();

                AuthResponse body = AuthResponse.builder()
                                .token(jwtTokenProvider.generateToken(user))
                                .tokenType("Bearer")
                                .refreshToken(newRefreshToken.getToken())
                                .user(modelMapper.map(user, UserResponse.class))
                                .build();

                return ResponseEntity.ok(ApiResponse.ok("Token refreshed", body));
        }

        /**
         * Revokes all refresh tokens for the authenticated user (logout from every
         * device).
         * Existing JWTs remain valid until they expire (~15 minutes).
         */
        @PostMapping("/logout-all")
        public ResponseEntity<ApiResponse<Void>> logoutAll(
                        @AuthenticationPrincipal User currentUser) {

                refreshTokenService.revokeAllUserTokens(currentUser);
                return ResponseEntity.ok(ApiResponse.ok("Logged out from all devices", null));
        }
}
