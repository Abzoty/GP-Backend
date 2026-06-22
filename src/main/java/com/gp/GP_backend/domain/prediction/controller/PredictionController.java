package com.gp.GP_backend.domain.prediction.controller;

import com.gp.GP_backend.domain.prediction.dto.PredictionRequest;
import com.gp.GP_backend.domain.prediction.dto.PredictionResponse;
import com.gp.GP_backend.domain.prediction.service.PredictionOrchestrationService;
import com.gp.GP_backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for department prediction endpoints.
 *
 * All endpoints require JWT authentication.
 *
 * Paths:
 * - POST /api/v1/predictions/department : get department prediction based on
 * questionnaire answers and course registrations
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
     * Get a department prediction based on questionnaire answers and courses.
     *
     * @param request prediction request (questionnaire answers + course codes)
     * @param user    authenticated user (context)
     * @return 200 OK with prediction response
     */
    @PostMapping("/department")
    public ResponseEntity<ApiResponse<PredictionResponse>> predictDepartment(
            @Valid @RequestBody PredictionRequest request,
            @AuthenticationPrincipal UserDetails user) {

        log.info("Prediction request from user: {}", user.getUsername());

        PredictionResponse response = orchestrationService.predict(request);

        return ResponseEntity.ok(ApiResponse.ok("Department prediction generated", response));
    }
}