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
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Personalized space recommendations")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class SpaceRecommendationController {

    private final SpaceRecommendationService recommendationService;

    /**
     * Returns a ranked list of personalized space recommendations for the authenticated user.
     *
     * <p>Three independent signals are blended:
     * <ol>
     *   <li><b>COURSE_MATCH</b> — active {@code COLLEGE_COURSE} spaces whose
     *       {@code courseCode} matches one of the caller's registered course codes.
     *       Provide course codes via {@code courseCodes}; omit to skip this layer.</li>
     *   <li><b>SOCIAL</b> — spaces joined by co-members of the caller's existing spaces
     *       (friends-of-friends social graph). Score is proportional to how many
     *       distinct co-members have already joined the candidate space.</li>
     *   <li><b>SIMILAR_CONTENT</b> — active non-course spaces whose name/description is
     *       textually similar (Jaccard coefficient ≥ 0.15) to at least one space the
     *       caller already belongs to.</li>
     * </ol>
     *
     * <p>Results are ranked:
     * <ol>
     *   <li>By number of signals that independently nominated the space (descending) —
     *       a space endorsed by all three layers is far more relevant than one found by a single layer.</li>
     *   <li>Then by combined score (descending) as a tie-breaker.</li>
     * </ol>
     *
     * @param topN        maximum number of recommendations to return (1–50, default 20).
     * @param currentUser the authenticated user, injected by Spring Security.
     * @return up to {@code topN} recommended spaces with scores and reasons.
     */
    @GetMapping("/spaces")
    @Operation(summary = "Get personalized space recommendations for the authenticated user")
    public ResponseEntity<ApiResponse<List<SpaceRecommendationResponse>>> getRecommendations(

            @Parameter(description = "Maximum number of recommendations (1–50, default 20)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50)
            int topN,

            @AuthenticationPrincipal User currentUser) {

        List<SpaceRecommendationResponse> recommendations = recommendationService.recommend(
                currentUser.getId(),
                topN);

        return ResponseEntity.ok(ApiResponse.ok("Recommendations retrieved successfully", recommendations));
    }
}
