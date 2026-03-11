package com.gp.GP_backend.domain.user.controller;

import com.gp.GP_backend.domain.user.dto.RefreshRequest;
import com.gp.GP_backend.domain.user.dto.UpdateProfileRequest;
import com.gp.GP_backend.domain.user.dto.UserResponse;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.service.RefreshTokenService;
import com.gp.GP_backend.domain.user.service.UserService;
import com.gp.GP_backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for the currently authenticated user's own account.
 *
 * <p>
 * All routes require a valid JWT (enforced by
 * {@link com.gp.GP_backend.config.SecurityConfig}).
 * The user is injected via {@code @AuthenticationPrincipal} after the
 * {@link com.gp.GP_backend.security.JwtAuthFilter} populates the
 * SecurityContext.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final ModelMapper modelMapper;

    /** Returns the profile of the currently authenticated user. */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> viewProfile(
            @AuthenticationPrincipal User currentUser) {

        // The entity is already loaded by the JWT filter — no additional DB query
        // needed
        UserResponse profile = modelMapper.map(currentUser, UserResponse.class);
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved", profile));
    }

    /**
     * Applies partial updates to the current user's profile.
     * Null fields in the request body are ignored (PATCH semantics).
     */
    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {

        User updated = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", modelMapper.map(updated, UserResponse.class)));
    }

    /**
     * Revokes the specific refresh token provided in the request body
     * (single-device logout).
     * The JWT access token remains valid until it expires naturally.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody RefreshRequest request) {

        refreshTokenService.revokeTokenForUser(request.getRefreshToken(), currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }
}
