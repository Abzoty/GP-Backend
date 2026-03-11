package com.gp.GP_backend.domain.user.controller;

import com.gp.GP_backend.domain.user.dto.*;
import com.gp.GP_backend.domain.user.entity.RefreshToken;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.RefreshTokenRepository;
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

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

        private final UserService userService;
        private final RefreshTokenService refreshTokenService;
        private final AuthenticationManager authenticationManager;
        private final JwtTokenProvider jwtTokenProvider;
        private final ModelMapper modelMapper;
        private final RefreshTokenRepository refreshTokenRepository;

        @PostMapping("/register")
        public ResponseEntity<ApiResponse<RegisterResponse>> register(
                        @Valid @RequestBody RegisterRequest request) {

                try {
                        User user = userService.registerUser(request);

                        RegisterResponse registerResponse = RegisterResponse.builder()
                                        .userId(user.getId())
                                        .email(user.getEmail())
                                        .fullName(user.getFullName())
                                        .build();

                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(ApiResponse.ok("Registration successful", registerResponse));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(ApiResponse.fail("A7A" + e.getMessage()));
                }

        }

        @PostMapping("/login")
        public ResponseEntity<ApiResponse<AuthResponse>> login(
                        @Valid @RequestBody LoginRequest request) {

                Authentication auth = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getEmail(), request.getPassword()));

                User user = (User) auth.getPrincipal();
                String token = jwtTokenProvider.generateToken(user);
                String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

                AuthResponse authResponse = AuthResponse.builder()
                                .token(token)
                                .tokenType("Bearer")
                                .refreshToken(refreshToken)
                                .userId(user.getId())
                                .email(user.getEmail())
                                .fullName(user.getFullName())
                                .build();

                return ResponseEntity.ok(ApiResponse.ok("Login successful", authResponse));
        }

        @PostMapping("/refresh")
        public ResponseEntity<ApiResponse<AuthResponse>> refresh(
                        @Valid @RequestBody RefreshRequest request) {

                // rotateRefreshToken handles validation, reuse detection, and rotation
                RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(
                                request.getRefreshToken());

                User user = (User) newRefreshToken.getUser();
                String newAccessToken = jwtTokenProvider.generateToken(user);

                AuthResponse authResponse = AuthResponse.builder()
                                .token(newAccessToken)
                                .tokenType("Bearer")
                                .refreshToken(newRefreshToken.getToken()) // ← new token each time
                                .user(modelMapper.map(user, UserResponse.class))
                                .build();

                return ResponseEntity.ok(ApiResponse.ok("Token refreshed", authResponse));
        }

        @PostMapping("/logout")
        public ResponseEntity<ApiResponse<Void>> logout(
                        @Valid @RequestBody @AuthenticationPrincipal User currentUser) {
                RefreshToken token = refreshTokenRepository.findByUser(currentUser)
                                .orElseThrow(() -> new IllegalArgumentException("No active session found"));
                refreshTokenService.revokeToken(token.getToken());
                return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
        }

        @PostMapping("/logout-all")
        public ResponseEntity<ApiResponse<Void>> logoutAll(
                        @Valid @RequestBody RefreshRequest request) {
                RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
                refreshTokenService.revokeAllUserTokens((User) token.getUser());
                return ResponseEntity.ok(ApiResponse.ok("Logged out from all devices", null));
        }
}