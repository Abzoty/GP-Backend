package com.gp.GP_backend.domain.recommendation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes ML-powered recommendation endpoints.
 *
 * TODO: Implement:
 * - GET /api/v1/recommendations/courses — returns personalised course
 * suggestions
 * - POST /api/v1/recommendations/department — predicts the best department
 * based on interests
 *
 * Both endpoints call the Python ML service via
 * {@link com.gp.GP_backend.config.WebClientConfig#mlRestClient()}.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    // TODO: inject CourseRecommendationService, DeptRecommendationService
}
