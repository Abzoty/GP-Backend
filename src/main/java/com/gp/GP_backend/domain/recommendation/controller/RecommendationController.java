package com.gp.GP_backend.domain.recommendation.controller;

import com.gp.GP_backend.domain.recommendation.dto.SpaceRecommendationResponse;
import com.gp.GP_backend.domain.recommendation.service.SpaceRecommendationService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Personalized space recommendations")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class RecommendationController {

    private final SpaceRecommendationService recommendationService;

    @GetMapping("/spaces")
    @Operation(summary = "Get personalized space recommendations for the authenticated user")
    public ResponseEntity<ApiResponse<List<SpaceRecommendationResponse>>> getRecommendations(
            @Parameter(description = "Maximum number of recommendations (1-50, default 20)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int topN,
            @AuthenticationPrincipal User currentUser) {

        List<SpaceRecommendationResponse> recommendations = recommendationService.recommend(currentUser.getId(), topN);
        return ResponseEntity.ok(ApiResponse.ok("Recommendations retrieved successfully", recommendations));
    }
}
