package com.gp.GP_backend.domain.prediction.controller;

import com.gp.GP_backend.domain.prediction.dto.PredictionRequest;
import com.gp.GP_backend.domain.prediction.dto.PredictionResponse;
import com.gp.GP_backend.domain.prediction.service.PredictionOrchestrationService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * - POST /api/v1/predictions/department
 * Body : { questionnaireAnswers: { 1: "1A", 2: "2C", ... } }
 * Courses are loaded automatically from the authenticated user's account.
 * Returns per-department scores from the questionnaire, the ML model,
 * and a weighted combination — structured for direct chart consumption.
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
     *
     * The client submits only questionnaire answers. The server fetches the
     * authenticated user's course registrations internally, calls the Python
     * ML service, combines the two score sources, and returns a response
     * ready for bar / radar chart rendering.
     *
     * @param user    the authenticated user — used to load course registrations
     * @param request questionnaire answers (question ID → answer choice ID)
     * @return 200 OK with {@link PredictionResponse}
     */
    @PostMapping("/department")
    public ResponseEntity<ApiResponse<PredictionResponse>> predictDepartment(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PredictionRequest request) {

        log.info("Prediction request received from user: {}", user.getUsername());

        PredictionResponse response = orchestrationService.predict(user, request);

        return ResponseEntity.ok(ApiResponse.ok("Department prediction generated", response));
    }
}