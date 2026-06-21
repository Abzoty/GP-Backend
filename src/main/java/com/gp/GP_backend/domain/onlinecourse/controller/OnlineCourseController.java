package com.gp.GP_backend.domain.onlinecourse.controller;

import com.gp.GP_backend.domain.onlinecourse.dto.OnlineCourseResponse;
import com.gp.GP_backend.domain.onlinecourse.service.OnlineCourseService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Online Courses")
@SecurityRequirement(name = "bearerAuth")
public class OnlineCourseController {

    private final OnlineCourseService onlineCourseService;

    @GetMapping("/courses")
    @Operation(summary = "Get online courses for the student's registered courses")
    public ResponseEntity<ApiResponse<List<OnlineCourseResponse>>> getCourses(
            @AuthenticationPrincipal User user) {

        List<OnlineCourseResponse> courses = onlineCourseService
            .getCoursesForStudent(user.getId());

        return ResponseEntity.ok(ApiResponse.ok("Courses retrieved successfully", courses));
    }
}