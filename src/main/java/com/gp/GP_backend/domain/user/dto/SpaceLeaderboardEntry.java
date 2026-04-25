package com.gp.GP_backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A single entry in a space-specific gamification leaderboard.
 *
 * <p>
 * The {@code xpPoints} and {@code level} fields reflect the user's
 * <em>overall</em> platform totals (same as the system leaderboard), while
 * the activity counters ({@code postsInSpace}, {@code answersInSpace},
 * {@code materialsSharedInSpace}) are scoped to the requested space only.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceLeaderboardEntry {

    /** 1-based position in the leaderboard, ordered by overall XP. */
    private Integer rank;

    private UUID userId;
    private String fullName;

    /** Overall XP across the entire platform. */
    private Integer xpPoints;

    /** Overall level derived from platform-wide XP. */
    private Short level;

    /** Number of posts the user has created in this space. */
    private Integer postsInSpace;

    /** Number of answers the user has submitted in this space. */
    private Integer answersInSpace;

    /** Number of materials the user has shared in this space. */
    private Integer materialsSharedInSpace;
}