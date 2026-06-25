package com.gp.GP_backend.domain.prediction.controller;

import com.gp.GP_backend.domain.prediction.dto.CourseValidationErrorDetails;
import com.gp.GP_backend.domain.prediction.dto.PredictionRequest;
import com.gp.GP_backend.domain.prediction.dto.PredictionResponse;
import com.gp.GP_backend.domain.prediction.exception.InsufficientCourseDataException;
import com.gp.GP_backend.domain.prediction.service.PredictionOrchestrationService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for department prediction endpoints.
 *
 * All endpoints require JWT authentication.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/predictions")
@RequiredArgsConstructor
@Slf4j
public class PredictionController {

    private final PredictionOrchestrationService orchestrationService;

    /**
     * Predict the most suitable department.
     */
    @PostMapping("/department")
    public ResponseEntity<ApiResponse<PredictionResponse>> predictDepartment(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PredictionRequest request) {

        log.info("Prediction request received from user: {}", user.getUsername());

        PredictionResponse response = orchestrationService.predict(user, request);

        return ResponseEntity.ok(ApiResponse.ok("Department prediction generated", response));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 422 handler — surfaces insufficient course data to the frontend
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Handles {@link InsufficientCourseDataException} thrown when the Python ML
     * service rejects the prediction because the student hasn't completed enough
     * courses.
     *
     * Returns HTTP 422 with a body listing exactly which courses are missing
     * and which are registered but still awaiting a final grade, so the
     * frontend can show an actionable message and redirect the student to
     * course registration.
     */
    @ExceptionHandler(InsufficientCourseDataException.class)
    public ResponseEntity<ApiResponse<CourseValidationErrorDetails>> handleInsufficientCourseData(
            InsufficientCourseDataException ex) {

        log.warn("Prediction rejected — missing={}, incomplete={}",
                ex.getMissingCourses(), ex.getIncompleteCourses());

        CourseValidationErrorDetails details = CourseValidationErrorDetails.builder()
                .error("INSUFFICIENT_COURSE_DATA")
                .missingCourses(ex.getMissingCourses())
                .incompleteCourses(ex.getIncompleteCourses())
                .build();

        return ResponseEntity
                .status(HttpStatus.valueOf(422))
                .body(ApiResponse.ok(
                        ex.getMessage() != null
                                ? ex.getMessage()
                                : "Insufficient course data for prediction",
                        details));
    }
}