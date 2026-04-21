package com.gp.GP_backend.domain.user.controller;

import com.gp.GP_backend.domain.user.dto.CourseRegistrationResponse;
import com.gp.GP_backend.domain.user.dto.RegisterCourseRequest;
import com.gp.GP_backend.domain.user.dto.UpdateCourseRegistrationRequest;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.service.CourseRegistrationService;
import com.gp.GP_backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints for managing a user's course registrations.
 *
 * <p>
 * All routes require a valid JWT (enforced by
 * {@link com.gp.GP_backend.config.SecurityConfig}).
 * The authenticated user is injected via {@code @AuthenticationPrincipal}
 * — their ID is never taken from the request body.
 */
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseRegistrationController {

    private final CourseRegistrationService courseRegistrationService;

    /**
     * Registers a new course for the authenticated user.
     * Returns {@code 201 CREATED} with the persisted registration.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CourseRegistrationResponse>> registerCourse(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody RegisterCourseRequest request) {

        CourseRegistrationResponse response = courseRegistrationService.registerCourse(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Course registered successfully", response));
    }

    /**
     * Partially updates an existing course registration (e.g. adding a grade
     * or archiving it by setting {@code isCurrent} to {@code false}).
     * Null fields in the request body are ignored (PATCH semantics).
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseRegistrationResponse>> updateRegistration(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateCourseRegistrationRequest request) {

        CourseRegistrationResponse response = courseRegistrationService.updateRegistration(id, currentUser, request);
        return ResponseEntity.ok(ApiResponse.ok("Course registration updated", response));
    }

    /**
     * Permanently removes a course registration.
     * Returns {@code 200 OK} on success.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRegistration(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        courseRegistrationService.deleteRegistration(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Course registration removed", null));
    }

    /**
     * Retrieves a specific course registration by its ID.
     * Returns {@code 200 OK} with the registration details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseRegistrationResponse>> getCourseRegistration(
        @PathVariable UUID id,
        @AuthenticationPrincipal User currentUser) {

        CourseRegistrationResponse response = courseRegistrationService.getCourseRegistration(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Course registration retrieved", response));
    }

    /**
     * Returns only the active ({@code isCurrent = true}) courses for the
     * authenticated user — intended for the current-semester dashboard view.
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<List<CourseRegistrationResponse>>> getCurrentCourses(
            @AuthenticationPrincipal User currentUser) {

        List<CourseRegistrationResponse> courses = courseRegistrationService.getCurrentCourses(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Current courses retrieved", courses));
    }

    /**
     * Returns the full enrollment history (current and archived) for the
     * authenticated user — useful for the profile/transcript view.
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<CourseRegistrationResponse>>> getAllCourses(
            @AuthenticationPrincipal User currentUser) {

        List<CourseRegistrationResponse> courses = courseRegistrationService.getAllCourses(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("All course registrations retrieved", courses));
    }
}