package com.gp.GP_backend.domain.user.controller;

import com.gp.GP_backend.domain.user.dto.*;
import com.gp.GP_backend.domain.user.entity.RefreshToken;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.service.GamificationService;
import com.gp.GP_backend.domain.user.service.PasswordResetService;
import com.gp.GP_backend.domain.user.service.RefreshTokenService;
import com.gp.GP_backend.domain.user.service.UserService;
import com.gp.GP_backend.security.JwtTokenProvider;
import com.gp.GP_backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Handles user registration, login, token refresh, logout, and password-reset
 * operations.
 *
 * <p>
 * All endpoints under {@code /api/v1/auth} are public (no JWT required).
 * See {@link com.gp.GP_backend.config.SecurityConfig} for the permit-list.
 *
 * <p>
 * Validation failures and business-logic exceptions are handled centrally by
 * {@link com.gp.GP_backend.shared.exception.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

        private final UserService userService;
        private final RefreshTokenService refreshTokenService;
        private final PasswordResetService passwordResetService;
        private final AuthenticationManager authenticationManager;
        private final JwtTokenProvider jwtTokenProvider;
        private final ModelMapper modelMapper;
        private final GamificationService gamificationService;

        /**
         * Registers a new user account.
         * Returns 201 CREATED with a minimal confirmation payload.
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
         * Spring Security's {@link AuthenticationManager} validates the credentials.
         */
        @PostMapping("/login")
        public ResponseEntity<ApiResponse<AuthResponse>> login(
                        @Valid @RequestBody LoginRequest request) {

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


                        //  Fix — isolate gamification from the auth result
                        try {
                        gamificationService.trackDailyLogin(user.getId());
                        } catch (Exception ex) {
                        Logger logger = LoggerFactory.getLogger(AuthController.class);
                        logger.warn("Gamification tracking failed for user {} — login still succeeds: {}",
                                user.getId(), ex.getMessage());

                        }

                return ResponseEntity.ok(ApiResponse.ok("Login successful", body));
        }

        /**
         * Exchanges a valid refresh token for a new access token and a rotated refresh
         * token.
         * The old token is invalidated on use; presenting a used token triggers full
         * session revocation.
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
         * Existing JWTs remain valid until they expire naturally (~15 minutes).
         */
        @PostMapping("/logout-all")
        public ResponseEntity<ApiResponse<Void>> logoutAll(
                        @AuthenticationPrincipal User currentUser) {

                refreshTokenService.revokeAllUserTokens(currentUser);
                return ResponseEntity.ok(ApiResponse.ok("Logged out from all devices", null));
        }

        // ─── Password reset ───────────────────────────────────────────────────────

        /**
         * Initiates the forgot-password flow by sending a reset link to the given
         * email.
         *
         * <p>
         * <b>Always returns 200</b> regardless of whether the email is registered.
         * This intentional ambiguity prevents user-enumeration attacks — callers
         * cannot tell from the response whether an account exists.
         */
        @PostMapping("/forgot-password")
        public ResponseEntity<ApiResponse<Void>> forgotPassword(
                        @Valid @RequestBody ForgotPasswordRequest request) {

                // Runs silently — no exception is surfaced even for unknown emails
                passwordResetService.initiateForgotPassword(request.getEmail());

                return ResponseEntity.ok(
                                ApiResponse.ok("If that email is registered, a reset link has been sent", null));
        }

        /**
         * Validates the reset token and applies the new password.
         * The token is single-use and expires 15 minutes after issuance.
         * All active sessions are revoked on success.
         */
        @PostMapping("/reset-password")
        public ResponseEntity<ApiResponse<Void>> resetPassword(
                        @Valid @RequestBody ResetPasswordRequest request) {

                passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
                return ResponseEntity.ok(ApiResponse.ok("Password reset successfully. Please log in again.", null));
        }
}