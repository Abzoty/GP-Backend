package com.gp.GP_backend.domain.user.controller;

import com.gp.GP_backend.domain.user.dto.AuthResponse;
import com.gp.GP_backend.domain.user.dto.LoginRequest;
import com.gp.GP_backend.domain.user.dto.RegisterRequest;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.service.UserService;
import com.gp.GP_backend.security.JwtTokenProvider;
import com.gp.GP_backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        try {
            User user = userService.registerUser(request);
            String token = jwtTokenProvider.generateToken(user);

            AuthResponse authResponse = AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .userId(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Registration successful", authResponse));
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

        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Login successful", authResponse));
    }
}