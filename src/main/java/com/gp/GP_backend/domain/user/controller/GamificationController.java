package com.gp.GP_backend.domain.user.controller;

import com.gp.GP_backend.domain.user.dto.GamificationProfileResponse;
import com.gp.GP_backend.domain.user.dto.SpaceLeaderboardEntry;
import com.gp.GP_backend.domain.user.dto.SystemLeaderboardEntry;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.service.GamificationService;
import com.gp.GP_backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints for gamification: the authenticated user's own profile,
 * the system-wide leaderboard, and per-space leaderboards.
 *
 * <p>
 * All routes require a valid JWT (enforced by
 * {@link com.gp.GP_backend.config.SecurityConfig}).
 */
@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
@Tag(name = "Gamification", description = "XP profiles, leaderboards, and streak information")
@SecurityRequirement(name = "bearerAuth")
public class GamificationController {

    private final GamificationService gamificationService;

    /**
     * Returns the full gamification profile (XP, level, streaks, counters)
     * for the currently authenticated user.
     */
    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's gamification profile")
    public ResponseEntity<ApiResponse<GamificationProfileResponse>> getMyProfile(
            @AuthenticationPrincipal User currentUser) {

        GamificationProfileResponse profile = gamificationService.getProfile(currentUser.getId());

        return ResponseEntity.ok(ApiResponse.ok("Gamification profile retrieved", profile));
    }

    /**
     * Returns the top {@code limit} users platform-wide, ranked by overall XP.
     *
     * @param limit maximum entries to return (default 20, max 100).
     */
    @GetMapping("/leaderboard")
    @Operation(summary = "Get the system-wide XP leaderboard")
    public ResponseEntity<ApiResponse<List<SystemLeaderboardEntry>>> getSystemLeaderboard(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {

        List<SystemLeaderboardEntry> leaderboard = gamificationService.getSystemLeaderboard(limit);

        return ResponseEntity.ok(ApiResponse.ok("System leaderboard retrieved", leaderboard));
    }

    /**
     * Returns all members of the given space ranked by their overall XP,
     * with space-scoped activity counters (posts, answers, materials).
     *
     * <p>
     * The requesting user must be a member of the space.
     *
     * @param spaceId the target space's UUID.
     * @param limit   maximum entries to return (default 20, max 100).
     */
    @GetMapping("/leaderboard/spaces/{spaceId}")
    @Operation(summary = "Get the leaderboard for a specific space")
    public ResponseEntity<ApiResponse<List<SpaceLeaderboardEntry>>> getSpaceLeaderboard(
            @PathVariable UUID spaceId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @AuthenticationPrincipal User currentUser) {

        List<SpaceLeaderboardEntry> leaderboard = gamificationService.getSpaceLeaderboard(spaceId, currentUser.getId(),
                limit);

        return ResponseEntity.ok(ApiResponse.ok("Space leaderboard retrieved", leaderboard));
    }
}