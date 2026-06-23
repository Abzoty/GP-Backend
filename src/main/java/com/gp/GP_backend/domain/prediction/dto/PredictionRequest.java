package com.gp.GP_backend.domain.prediction.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request DTO for the department prediction endpoint.
 *
 * The client passes the normalized scores it already received from
 * POST /api/v1/questionnaire/score (the "data.normalized" field).
 * Course registrations are fetched server-side automatically.
 *
 * Example body:
 * {
 * "normalizedScores": {
 * "AI": 0.1333,
 * "CS": 0.2444,
 * "IT": 0.1000,
 * "IS": 0.3556,
 * "DS": 0.1667
 * }
 * }
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionRequest {

    /**
     * Normalized department scores from the questionnaire scoring endpoint.
     * Taken directly from the "data.normalized" field of the /score response.
     * Values must be in [0, 1] and should sum to ~1.0.
     */
    @NotNull(message = "Normalized scores are required")
    @NotEmpty(message = "Normalized scores cannot be empty")
    private Map<String, BigDecimal> normalizedScores;
}