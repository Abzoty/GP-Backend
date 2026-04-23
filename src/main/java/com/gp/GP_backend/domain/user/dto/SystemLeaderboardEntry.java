package com.gp.GP_backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A single entry in the system-wide gamification leaderboard.
 *
 * <p>
 * All counters ({@code totalPosts}, {@code totalAnswers},
 * {@code totalMaterialsShared}) reflect the user's lifetime totals across the
 * entire platform.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLeaderboardEntry {

    /** 1-based position in the leaderboard. */
    private Integer rank;

    private UUID userId;
    private String fullName;

    /** Total accumulated XP across all activities. */
    private Integer xpPoints;

    /** Current level derived from {@code xpPoints}. */
    private Short level;

    /** Lifetime posts created on the platform. */
    private Integer totalPosts;

    /** Lifetime answers submitted on the platform. */
    private Integer totalAnswers;

    /** Lifetime materials shared across all spaces. */
    private Integer totalMaterialsShared;
}