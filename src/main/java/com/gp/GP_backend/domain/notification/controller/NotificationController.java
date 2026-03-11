package com.gp.GP_backend.domain.notification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
