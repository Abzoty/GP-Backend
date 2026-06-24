package com.gp.GP_backend.domain.course.controller;

import com.gp.GP_backend.domain.course.dto.CourseRegistrationRequest;
import com.gp.GP_backend.domain.course.dto.CourseRegistrationResponse;
import com.gp.GP_backend.domain.course.dto.CourseRegistrationUpdateRequest;
import com.gp.GP_backend.domain.course.service.CourseRegistrationService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for course registration endpoints.
 *
 * All endpoints require JWT authentication (user context
 * from @AuthenticationPrincipal User).
 *
 * Paths:
 * - POST /api/v1/courses : register a new course
 * - GET /api/v1/courses : get all registrations (current + closed)
 * - GET /api/v1/courses/current : get current (non-closed) registrations
 * - GET /api/v1/courses/{id} : get a single registration
 * - PATCH /api/v1/courses/{id} : update a registration
 * - DELETE /api/v1/courses/{id} : delete a registration
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Slf4j
public class CourseRegistrationController {

    private final CourseRegistrationService courseRegistrationService;

    /**
     * Register a new course.
     *
     * @param user    the authenticated user
     * @param request the registration request (code, termWork, examWork)
     * @return 201 Created with the created registration
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CourseRegistrationResponse>> registerCourse(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CourseRegistrationRequest request) {

        CourseRegistrationResponse response = courseRegistrationService.registerCourse(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Course registered successfully", response));
    }

    /**
     * Get all course registrations for the authenticated user.
     * Includes both current and closed registrations.
     *
     * @param user the authenticated user
     * @return 200 OK with list of all registrations
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseRegistrationResponse>>> getAllCourses(
            @AuthenticationPrincipal User user) {

        List<CourseRegistrationResponse> response = courseRegistrationService.getAllCourses(user);
        return ResponseEntity.ok(ApiResponse.ok("All courses retrieved", response));
    }

    /**
     * Get current (non-closed) course registrations for the authenticated user.
     *
     * @param user the authenticated user
     * @return 200 OK with list of current registrations
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<List<CourseRegistrationResponse>>> getCurrentCourses(
            @AuthenticationPrincipal User user) {

        List<CourseRegistrationResponse> response = courseRegistrationService.getCurrentCourses(user);
        return ResponseEntity.ok(ApiResponse.ok("Current courses retrieved", response));
    }

    /**
     * Get a specific course registration by ID.
     *
     * @param user the authenticated user
     * @param id   the registration ID
     * @return 200 OK with the registration
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseRegistrationResponse>> getRegistration(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        CourseRegistrationResponse response = courseRegistrationService.getRegistration(user, id);
        return ResponseEntity.ok(ApiResponse.ok("Course registration retrieved", response));
    }

    /**
     * Update a course registration (PATCH semantics).
     * Only non-null fields in the request are updated.
     *
     * @param user    the authenticated user
     * @param id      the registration ID
     * @param request the update request
     * @return 200 OK with the updated registration
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseRegistrationResponse>> updateRegistration(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody CourseRegistrationUpdateRequest request) {

        CourseRegistrationResponse response = courseRegistrationService.updateRegistration(user, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Course registration updated", response));
    }

    /**
     * Delete a course registration.
     *
     * @param user the authenticated user
     * @param id   the registration ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRegistration(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        courseRegistrationService.deleteRegistration(user, id);
        return ResponseEntity.noContent().build();
    }
}