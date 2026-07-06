package com.gp.GP_backend.domain.user.controller;

import com.gp.GP_backend.domain.user.dto.ChangePasswordRequest;
import com.gp.GP_backend.domain.user.dto.RefreshRequest;
import com.gp.GP_backend.domain.user.dto.UpdateProfileRequest;
import com.gp.GP_backend.domain.user.dto.UserResponse;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.service.PasswordResetService;
import com.gp.GP_backend.domain.user.service.RefreshTokenService;
import com.gp.GP_backend.domain.user.service.UserService;
import com.gp.GP_backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService passwordResetService;
    private final ModelMapper modelMapper;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> viewProfile(
            @AuthenticationPrincipal User currentUser) {

        UserResponse profile = modelMapper.map(currentUser, UserResponse.class);
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved", profile));
    }


    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {

        User updated = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", modelMapper.map(updated, UserResponse.class)));
    }


    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {

        passwordResetService.changePassword(
                currentUser, request.getCurrentPassword(), request.getNewPassword());

        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully. Please log in again.", null));
    }


    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody RefreshRequest request) {

        refreshTokenService.revokeTokenForUser(request.getRefreshToken(), currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }
}