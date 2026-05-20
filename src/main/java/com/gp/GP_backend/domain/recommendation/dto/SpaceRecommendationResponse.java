package com.gp.GP_backend.domain.recommendation.dto;

import com.gp.GP_backend.domain.space.dto.SpaceResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceRecommendationResponse {

    /** The recommended space, fully populated. */
    private SpaceResponse space;

    /**
     * Number of independent recommendation layers (1–3) that nominated this space.
     * A space nominated by all three layers is a much stronger signal than one
     * found by only a single layer.
     */
    private int methodCount;

    /**
     * Combined score, computed as the sum of each layer's normalized contribution
     * (each in [0.0, 1.0]). Used as a tie-breaker within the same {@code methodCount}.
     */
    private double score;

    /**
     * Human-readable labels indicating which layers contributed to this recommendation.
     * Possible values:
     * <ul>
     *   <li>{@code "COURSE_MATCH"}    — matches the user's registered course codes.</li>
     *   <li>{@code "SOCIAL"}          — joined by members of the user's current spaces.</li>
     *   <li>{@code "SIMILAR_CONTENT"} — textually similar to spaces the user already belongs to.</li>
     * </ul>
     */
    private Set<String> reasons;
}
