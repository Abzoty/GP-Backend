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

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Slf4j
public class CourseRegistrationController {

    private final CourseRegistrationService courseRegistrationService;

    @PostMapping
    public ResponseEntity<ApiResponse<CourseRegistrationResponse>> registerCourse(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CourseRegistrationRequest request) {

        CourseRegistrationResponse response = courseRegistrationService.registerCourse(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Course registered successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseRegistrationResponse>>> getAllCourses(
            @AuthenticationPrincipal User user) {

        List<CourseRegistrationResponse> response = courseRegistrationService.getAllCourses(user);
        return ResponseEntity.ok(ApiResponse.ok("All courses retrieved", response));
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<List<CourseRegistrationResponse>>> getCurrentCourses(
            @AuthenticationPrincipal User user) {

        List<CourseRegistrationResponse> response = courseRegistrationService.getCurrentCourses(user);
        return ResponseEntity.ok(ApiResponse.ok("Current courses retrieved", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseRegistrationResponse>> getRegistration(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        CourseRegistrationResponse response = courseRegistrationService.getRegistration(user, id);
        return ResponseEntity.ok(ApiResponse.ok("Course registration retrieved", response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseRegistrationResponse>> updateRegistration(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody CourseRegistrationUpdateRequest request) {

        CourseRegistrationResponse response = courseRegistrationService.updateRegistration(user, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Course registration updated", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRegistration(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        courseRegistrationService.deleteRegistration(user, id);
        return ResponseEntity.noContent().build();
    }
}