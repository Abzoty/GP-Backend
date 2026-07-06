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
import lombok.extern.slf4j.Slf4j;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

        private final UserService userService;
        private final RefreshTokenService refreshTokenService;
        private final PasswordResetService passwordResetService;
        private final AuthenticationManager authenticationManager;
        private final JwtTokenProvider jwtTokenProvider;
        private final ModelMapper modelMapper;
        private final GamificationService gamificationService;

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

                try {
                        gamificationService.trackDailyLogin(user.getId());
                } catch (Exception ex) {
                        log.warn("Gamification tracking failed ...", user.getId(), ex.getMessage());
                }

                return ResponseEntity.ok(ApiResponse.ok("Login successful", body));
        }

        
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


        @PostMapping("/logout-all")
        public ResponseEntity<ApiResponse<Void>> logoutAll(
                        @AuthenticationPrincipal User currentUser) {

                refreshTokenService.revokeAllUserTokens(currentUser);
                return ResponseEntity.ok(ApiResponse.ok("Logged out from all devices", null));
        }


        @PostMapping("/forgot-password")
        public ResponseEntity<ApiResponse<Void>> forgotPassword(
                        @Valid @RequestBody ForgotPasswordRequest request) {

                passwordResetService.initiateForgotPassword(request.getEmail());

                return ResponseEntity.ok(
                                ApiResponse.ok("If that email is registered, a reset link has been sent", null));
        }


        @PostMapping("/reset-password")
        public ResponseEntity<ApiResponse<Void>> resetPassword(
                        @Valid @RequestBody ResetPasswordRequest request) {

                passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
                return ResponseEntity.ok(ApiResponse.ok("Password reset successfully. Please log in again.", null));
        }
}