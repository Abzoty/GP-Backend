package com.gp.GP_backend.domain.notification.controller;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gp.GP_backend.domain.notification.dto.NotificationResponse;
import com.gp.GP_backend.domain.notification.service.NotificationService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.response.ApiResponse;
import com.gp.GP_backend.shared.response.PagedResponse;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;




/**
 * REST controller for Notification endpoints.
 *
 * TODO: Implement CRUD endpoints once NotificationService is complete.
 * All endpoints here are protected by JWT (configured in SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    // TODO: inject NotificationService
    private final NotificationService notificationService;

    @GetMapping("/api/v1/notifications/all-notifications")
    @Operation(summary = "Get notifications for a user with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<NotificationResponse> notifications = notificationService.getNotificationsForUser(user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.ok("Notifications fetched successfully", PagedResponse.of(notifications)));
    }

    @PutMapping("/api/v1/notifications/mark-read/{notificationId}")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markNotificationAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable UUID notificationId
    ) {
        notificationService.markNotificationAsRead(user.getId(), notificationId);
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read successfully", null));
    }


    @PutMapping("/api/v1/notifications/toggle-email")
    @Operation(summary = "Toggle email notifications")
    public ResponseEntity<ApiResponse<Void>>toggleEmailNotificationAvailability(
            @AuthenticationPrincipal User user
    ) {
        notificationService.toggleEmailNotifications(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Email Notification toggled successfully", null));
    }


    @PutMapping("/api/v1/notifications/toggle-inapp")
    @Operation(summary = "Toggle email notifications")
    public ResponseEntity<ApiResponse<Void>>toggleAppNotificationAvailability(
            @AuthenticationPrincipal User user
    ) {
        notificationService.toggleInAppNotifications(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("App Notification toggled successfully", null));
    }
}
