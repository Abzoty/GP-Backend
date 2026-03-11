package com.gp.GP_backend.domain.user.controller;

import com.gp.GP_backend.domain.user.dto.UserResponse;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.service.UserService;
import com.gp.GP_backend.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Handles requests related to the authenticated user's own profile.
 *
 * <p>
 * All endpoints require a valid JWT Bearer token (enforced globally by
 * {@link com.gp.GP_backend.config.SecurityConfig}).
 *
 * <p>
 * {@code @AuthenticationPrincipal} injects the {@link User} entity that was
 * placed in the
 * {@link org.springframework.security.core.context.SecurityContext}
 * by {@link com.gp.GP_backend.security.JwtAuthFilter} — no extra DB call
 * needed.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ModelMapper modelMapper;

    /**
     * Returns the current user's profile.
     *
     * <p>
     * Uses the principal already in the security context — avoids a redundant DB
     * lookup
     * since {@link com.gp.GP_backend.security.JwtAuthFilter} already loaded the
     * full
     * {@link User} entity during JWT validation.
     *
     * @param currentUser injected from the security context by
     *                    {@code @AuthenticationPrincipal}
     */
    @GetMapping("/profile/view")
    public ResponseEntity<ApiResponse<UserResponse>> viewProfile(
            @AuthenticationPrincipal User currentUser) {

        UserResponse body = modelMapper.map(currentUser, UserResponse.class);
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved", body));
    }
}